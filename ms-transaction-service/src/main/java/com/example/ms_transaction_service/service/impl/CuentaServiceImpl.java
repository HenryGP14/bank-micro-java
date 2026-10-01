package com.example.ms_transaction_service.service.impl;

import com.example.ms_transaction_service.dto.CuentaRequest;
import com.example.ms_transaction_service.dto.CuentaResponse;
import com.example.ms_transaction_service.dto.PageResponse;
import com.example.ms_transaction_service.entity.Cuenta;
import com.example.ms_transaction_service.entity.TipoCuenta;
import com.example.ms_transaction_service.exception.ResourceNotFoundException;
import com.example.ms_transaction_service.grpc.dto.ClientDto;
import com.example.ms_transaction_service.repository.CuentaRepository;
import com.example.ms_transaction_service.repository.TipoCuentaRepository;
import com.example.ms_transaction_service.service.AuditoriaService;
import com.example.ms_transaction_service.service.ClientGrpcClientService;
import com.example.ms_transaction_service.service.CuentaService;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de {@link CuentaService}. Antes de crear una cuenta valida contra
 * ms-client-service (via gRPC) que el cliente exista y este activo; toda operacion sobre una
 * cuenta existente reconfirma ademas que sea del cliente autenticado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private static final String CUENTA_NUMERO_DUPLICADO =
            "Lo sentimos, la cuenta ya se encuentra registrada.";

    private static final String ENTIDAD_CUENTA = "CUENTA";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CuentaRepository cuentaRepository;
    private final TipoCuentaRepository tipoCuentaRepository;
    private final ClientGrpcClientService clientGrpcClientService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> findAllByCliente(Long clienteId) {
        return cuentaRepository.findByClienteId(clienteId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CuentaResponse> findAllByCliente(Long clienteId, Pageable pageable) {
        return PageResponse.from(cuentaRepository.findByClienteId(clienteId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse findByIdAndCliente(Long id, Long clienteId) {
        return toResponse(getCuentaPropia(id, clienteId));
    }

    /**
     * Abre la cuenta: primero confirma via gRPC que el cliente exista y este activo, luego
     * resuelve el numero de cuenta (manual o generado aleatoriamente) antes de guardar.
     */
    @Override
    @Transactional
    public CuentaResponse create(Long clienteId, CuentaRequest request) {
        clientGrpcClientService.validateClientOrThrow(clienteId);

        TipoCuenta tipoCuenta = tipoCuentaRepository.findByDescripcionIgnoreCase(request.getTipoCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cuenta no encontrado: " + request.getTipoCuenta()));

        String numeroCuenta = resolverNumeroCuenta(request);
        log.info("Cuenta creada con numero {}: {}", Boolean.FALSE.equals(request.getAleatorio()) ? "manual" : "generado", numeroCuenta);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setTipoCuenta(tipoCuenta);
        cuenta.setSaldoInicial(request.getSaldoInicial());
        cuenta.setEstado(request.getEstado());
        cuenta.setClienteId(clienteId);
        cuenta = cuentaRepository.save(cuenta);
        auditoriaService.registrar(ENTIDAD_CUENTA, cuenta.getIdCuenta(), "CREAR", "Creado");
        return toResponse(cuenta);
    }

    /**
     * Reemplaza tipoCuenta, saldoInicial y estado por los valores del request (todos
     * obligatorios por validacion en {@code CuentaRequest}); a diferencia de partialUpdate, un
     * campo ausente aqui ya fallo antes de llegar al service.
     */
    @Override
    @Transactional
    public CuentaResponse update(Long id, Long clienteId, CuentaRequest request) {
        Cuenta cuenta = getCuentaPropia(id, clienteId);
        TipoCuenta tipoCuenta = tipoCuentaRepository.findByDescripcionIgnoreCase(request.getTipoCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cuenta no encontrado: " + request.getTipoCuenta()));
        cuenta.setTipoCuenta(tipoCuenta);
        cuenta.setSaldoInicial(request.getSaldoInicial());
        cuenta.setEstado(request.getEstado());
        cuenta = cuentaRepository.save(cuenta);
        auditoriaService.registrar(ENTIDAD_CUENTA, cuenta.getIdCuenta(), "ACTUALIZAR", "Actualizado");
        return toResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse partialUpdate(Long id, Long clienteId, CuentaRequest request) {
        Cuenta cuenta = getCuentaPropia(id, clienteId);
        if (request.getTipoCuenta() != null) {
            TipoCuenta tipoCuenta = tipoCuentaRepository.findByDescripcionIgnoreCase(request.getTipoCuenta())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de cuenta no encontrado"));
            cuenta.setTipoCuenta(tipoCuenta);
        }
        if (request.getSaldoInicial() != null) {
            cuenta.setSaldoInicial(request.getSaldoInicial());
        }
        if (request.getEstado() != null) {
            cuenta.setEstado(request.getEstado());
        }
        cuenta = cuentaRepository.save(cuenta);
        auditoriaService.registrar(ENTIDAD_CUENTA, cuenta.getIdCuenta(), "ACTUALIZAR", "Actualizado");
        return toResponse(cuenta);
    }

    /**
     * Baja logica; solo marca {@code estado=false} para no perder el historial de
     * movimientos asociados.
     */
    @Override
    @Transactional
    public void delete(Long id, Long clienteId) {
        Cuenta cuenta = getCuentaPropia(id, clienteId);
        cuenta.setEstado(false);
        cuentaRepository.save(cuenta);
        auditoriaService.registrar(ENTIDAD_CUENTA, id, "ELIMINAR", "Eliminado");
    }

    @Override
    @Transactional(readOnly = true)
    public Cuenta getCuenta(Long id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + id));
    }

    /**
     * Si la cuenta existe pero es de otro cliente, se responde igual que si no existiera
     * (404), para no revelar a un usuario que un numero de cuenta ajeno esta en uso.
     */
    private Cuenta getCuentaPropia(Long id, Long clienteId) {
        Cuenta cuenta = getCuenta(id);
        if (!cuenta.getClienteId().equals(clienteId)) {
            throw new ResourceNotFoundException("Cuenta no encontrada con id: " + id);
        }
        return cuenta;
    }

    @Override
    @Transactional(readOnly = true)
    public Cuenta getCuentaByNumero(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada: " + numeroCuenta));
    }

    private String resolverNumeroCuenta(CuentaRequest request) {
        if (Boolean.FALSE.equals(request.getAleatorio())) {
            String numero = request.getNumeroCuenta().trim();
            ensureNumeroCuentaDisponible(numero);
            return numero;
        }
        return generarNumeroCuentaUnico();
    }

    private void ensureNumeroCuentaDisponible(String numeroCuenta) {
        if (cuentaRepository.findByNumeroCuenta(numeroCuenta).isPresent()) {
            throw new IllegalArgumentException(CUENTA_NUMERO_DUPLICADO);
        }
    }

    /**
     * Sortea numeros de {@code NUMERO_CUENTA_LONGITUD} digitos hasta encontrar uno libre;
     * con el volumen actual de cuentas la colision es rara, pero por eso reintenta en loop
     * en vez de fallar a la primera.
     */
    private String generarNumeroCuentaUnico() {
        String numero;
        do {
            numero = String.format("%0" + CuentaRequest.NUMERO_CUENTA_LONGITUD + "d", RANDOM.nextInt(1_000_000));
        } while (cuentaRepository.findByNumeroCuenta(numero).isPresent());
        return numero;
    }

    /**
     * Enriquece la respuesta con el nombre del cliente pidiendolo por gRPC; si
     * ms-client-service no responde, no se rompe la consulta de cuentas, simplemente el
     * nombre queda null.
     */
    private CuentaResponse toResponse(Cuenta cuenta) {
        String nombreCliente = null;
        try {
            ClientDto client = clientGrpcClientService.getClientById(cuenta.getClienteId());
            if (client.encontrado()) {
                nombreCliente = client.nombre();
            }
        } catch (Exception ignored) {
            // Respuesta parcial si el servicio de clientes no esta disponible
        }
        return CuentaResponse.builder()
                .idCuenta(cuenta.getIdCuenta())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .tipoCuenta(cuenta.getTipoCuenta().getDescripcion())
                .saldoInicial(cuenta.getSaldoInicial())
                .estado(cuenta.getEstado())
                .clienteId(cuenta.getClienteId())
                .nombreCliente(nombreCliente)
                .build();
    }
}
