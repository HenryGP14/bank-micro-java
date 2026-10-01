package com.example.ms_transaction_service.service.impl;

import com.example.ms_transaction_service.dto.MovimientoRequest;
import com.example.ms_transaction_service.dto.MovimientoResponse;
import com.example.ms_transaction_service.dto.PageResponse;
import com.example.ms_transaction_service.entity.Cuenta;
import com.example.ms_transaction_service.entity.Movimiento;
import com.example.ms_transaction_service.entity.TipoMovimiento;
import com.example.ms_transaction_service.exception.ForbiddenOperationException;
import com.example.ms_transaction_service.exception.ResourceNotFoundException;
import com.example.ms_transaction_service.exception.SaldoNoDisponibleException;
import com.example.ms_transaction_service.repository.MovimientoRepository;
import com.example.ms_transaction_service.repository.TipoMovimientoRepository;
import com.example.ms_transaction_service.service.AuditoriaService;
import com.example.ms_transaction_service.service.ClientGrpcClientService;
import com.example.ms_transaction_service.service.CuentaService;
import com.example.ms_transaction_service.service.MovimientoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de {@link MovimientoService}. Aqui vive la regla de negocio mas delicada del
 * sistema: un deposito lo puede registrar cualquier cliente autenticado sobre cualquier
 * cuenta activa, pero un retiro solo lo puede hacer el dueno de la cuenta. Toda reversion
 * genera un movimiento nuevo en sentido contrario, nunca borra el original.
 */
@Service
@RequiredArgsConstructor
public class MovimientoServiceImpl implements MovimientoService {

    private static final String ENTIDAD_MOVIMIENTO = "MOVIMIENTO";

    private final MovimientoRepository movimientoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final CuentaService cuentaService;
    private final ClientGrpcClientService clientGrpcClientService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovimientoResponse> findByNumeroCuenta(String numeroCuenta, Long clienteId, Pageable pageable) {
        Cuenta cuenta = cuentaService.getCuentaByNumero(numeroCuenta);
        if (!cuenta.getClienteId().equals(clienteId)) {
            throw new ResourceNotFoundException("Cuenta no encontrada: " + numeroCuenta);
        }
        return PageResponse.from(movimientoRepository.findByCuenta_NumeroCuenta(numeroCuenta, pageable)
                .map(this::toResponse));
    }

    /**
     * Registra un deposito o un retiro. Solo el retiro exige que la cuenta sea del cliente
     * autenticado; un deposito se puede hacer a cualquier cuenta activa (por ejemplo,
     * alguien depositando en la cuenta de otra persona). Un retiro que dejaria saldo
     * negativo se rechaza antes de escribir nada.
     */
    @Override
    @Transactional
    public MovimientoResponse create(Long clienteId, MovimientoRequest request) {
        Cuenta cuenta = cuentaService.getCuentaByNumero(request.getNumeroCuenta());
        if (!Boolean.TRUE.equals(cuenta.getEstado())) {
            throw new IllegalArgumentException("La cuenta no esta activa");
        }

        if ("Retiro".equalsIgnoreCase(request.getTipoMovimiento()) && !cuenta.getClienteId().equals(clienteId)) {
            throw new ForbiddenOperationException("No tiene permisos para retirar de una cuenta que no es suya");
        }

        clientGrpcClientService.validateClientOrThrow(cuenta.getClienteId());

        TipoMovimiento tipoMovimiento = tipoMovimientoRepository
                .findByDescripcionIgnoreCase(request.getTipoMovimiento())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de movimiento no encontrado: " + request.getTipoMovimiento()));

        BigDecimal valor = normalizeValor(request.getValor(), tipoMovimiento.getDescripcion());
        BigDecimal nuevoSaldo = cuenta.getSaldoInicial().add(valor);
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaldoNoDisponibleException();
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setFecha(request.getFecha() != null ? request.getFecha() : LocalDateTime.now());
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setValor(valor);
        movimiento.setSaldo(nuevoSaldo);
        movimiento.setCuenta(cuenta);

        cuenta.setSaldoInicial(nuevoSaldo);
        Movimiento saved = movimientoRepository.save(movimiento);
        auditoriaService.registrar(ENTIDAD_MOVIMIENTO, saved.getIdMovimiento(), "CREAR", "Creado");
        return toResponse(saved);
    }

    /**
     * Unica edicion permitida sobre un movimiento existente: el motivo/descripcion. No toca
     * valor ni saldo, asi que no requiere revalidar saldo disponible.
     */
    @Override
    @Transactional
    public MovimientoResponse actualizarMotivo(Long id, Long clienteId, String motivo) {
        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado con id: " + id));
        if (!movimiento.getCuenta().getClienteId().equals(clienteId)) {
            throw new ResourceNotFoundException("Movimiento no encontrado con id: " + id);
        }
        movimiento.setMotivo(motivo);
        Movimiento actualizado = movimientoRepository.save(movimiento);
        auditoriaService.registrar(ENTIDAD_MOVIMIENTO, actualizado.getIdMovimiento(), "ACTUALIZAR", "Actualizado");
        return toResponse(actualizado);
    }

    /**
     * No borra el movimiento original: crea uno nuevo con el valor invertido (para que el
     * historial de saldos quede completo y auditable) y aplica el mismo chequeo de saldo
     * disponible que un retiro normal.
     * <p>
     * Es idempotente: si {@code id} ya tiene una reversion creada por una llamada anterior, no
     * crea una segunda (ver {@code movimientoOrigenId}). Sin este chequeo, reintentar un DELETE
     * que si llego al servidor pero cuya respuesta se perdio en la red duplicaria el efecto
     * sobre el saldo, violando la idempotencia que HTTP espera de DELETE.
     */
    @Override
    @Transactional
    public void revertir(Long id, Long clienteId, String motivo) {
        if (id == null || clienteId == null || motivo == null) {
            throw new ResourceNotFoundException("Parametros requeridos no encontrados");
        }
        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado con id: " + id));
        if (!movimiento.getCuenta().getClienteId().equals(clienteId)) {
            throw new ResourceNotFoundException("Movimiento no encontrado con id: " + id);
        }
        if (movimientoRepository.findByMovimientoOrigenId(id).isPresent()) {
            return;
        }

        Cuenta cuenta = movimiento.getCuenta();
        BigDecimal valorReverso = movimiento.getValor().negate();
        BigDecimal saldoRevertido = cuenta.getSaldoInicial().add(valorReverso);
        if (saldoRevertido.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaldoNoDisponibleException();
        }
        cuenta.setSaldoInicial(saldoRevertido);

        Movimiento reverso = new Movimiento();
        reverso.setFecha(LocalDateTime.now());
        reverso.setTipoMovimiento(movimiento.getTipoMovimiento());
        reverso.setValor(valorReverso);
        reverso.setSaldo(saldoRevertido);
        reverso.setCuenta(cuenta);
        reverso.setMotivo(motivo);
        reverso.setMovimientoOrigenId(movimiento.getIdMovimiento());
        Movimiento reversoGuardado = movimientoRepository.save(reverso);
        auditoriaService.registrar(ENTIDAD_MOVIMIENTO, reversoGuardado.getIdMovimiento(), "REVERTIR", "Revertido");
    }

    /**
     * Fuerza el signo del valor segun el tipo: un retiro siempre se guarda en negativo aunque
     * el request lo mande en positivo, para que sumar el valor al saldo funcione igual para
     * depositos y retiros.
     */
    private BigDecimal normalizeValor(BigDecimal valor, String tipoMovimiento) {
        if (valor == null) {
            throw new IllegalArgumentException("El valor del movimiento es obligatorio");
        }
        if ("Retiro".equalsIgnoreCase(tipoMovimiento) && valor.signum() > 0) {
            return valor.negate();
        }
        return valor;
    }

    private MovimientoResponse toResponse(Movimiento movimiento) {
        return MovimientoResponse.builder()
                .idMovimiento(movimiento.getIdMovimiento())
                .fecha(movimiento.getFecha())
                .numeroCuenta(movimiento.getCuenta().getNumeroCuenta())
                .tipoMovimiento(movimiento.getTipoMovimiento().getDescripcion())
                .valor(movimiento.getValor())
                .saldo(movimiento.getSaldo())
                .motivo(movimiento.getMotivo())
                .build();
    }
}
