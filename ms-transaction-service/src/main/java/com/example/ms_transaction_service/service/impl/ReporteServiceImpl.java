package com.example.ms_transaction_service.service.impl;

import com.example.ms_transaction_service.dto.ReporteCuentaResponse;
import com.example.ms_transaction_service.dto.ReporteEstadoCuentaResponse;
import com.example.ms_transaction_service.entity.Movimiento;
import com.example.ms_transaction_service.exception.ResourceNotFoundException;
import com.example.ms_transaction_service.grpc.dto.ClientDto;
import com.example.ms_transaction_service.repository.MovimientoRepository;
import com.example.ms_transaction_service.service.ClientGrpcClientService;
import com.example.ms_transaction_service.service.CuentaService;
import com.example.ms_transaction_service.service.ReporteService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de {@link ReporteService}. El nombre y la validez del cliente siempre se
 * confirman contra ms-client-service; los movimientos y saldos son locales de este
 * microservicio.
 */
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final MovimientoRepository movimientoRepository;
    private final ClientGrpcClientService clientGrpcClientService;
    private final CuentaService cuentaService;

    /**
     * Trae los movimientos del rango de fechas y, para cada uno, reconstruye el saldo previo
     * restando el valor del movimiento al saldo que quedo tras aplicarlo
     * ({@code saldo - valor = saldo anterior}), ya que solo se persiste el saldo resultante.
     */
    @Override
    @Transactional(readOnly = true)
    public ReporteEstadoCuentaResponse generarReporte(Long clienteId, LocalDate fechaInicio, LocalDate fechaFin) {
        ClientDto cliente = clientGrpcClientService.getClientById(clienteId);
        if (!cliente.encontrado()) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId);
        }

        List<Movimiento> movimientos = movimientoRepository.findByClienteAndFechaBetween(
                clienteId, fechaInicio.atStartOfDay(), fechaFin.atTime(LocalTime.MAX));

        List<ReporteCuentaResponse> detalle = new ArrayList<>();
        for (Movimiento movimiento : movimientos) {
            var cuenta = movimiento.getCuenta();
            BigDecimal saldoInicialMovimiento = movimiento.getSaldo().subtract(movimiento.getValor());
            detalle.add(ReporteCuentaResponse.builder()
                    .fecha(movimiento.getFecha())
                    .cliente(cliente.nombre())
                    .numeroCuenta(cuenta.getNumeroCuenta())
                    .tipo(cuenta.getTipoCuenta().getDescripcion())
                    .saldoInicial(saldoInicialMovimiento)
                    .estado(cuenta.getEstado())
                    .movimiento(movimiento.getValor())
                    .saldoDisponible(movimiento.getSaldo())
                    .motivo(movimiento.getMotivo())
                    .build());
        }

        return ReporteEstadoCuentaResponse.builder()
                .clienteId(clienteId)
                .nombreCliente(cliente.nombre())
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .cuentas(cuentaService.findAllByCliente(clienteId))
                .detalleMovimientos(detalle)
                .build();
    }

    /**
     * Resuelve primero el clienteId por identificacion contra ms-client-service y delega en
     * {@link #generarReporte}; evita duplicar la logica de armado del reporte.
     */
    @Override
    @Transactional(readOnly = true)
    public ReporteEstadoCuentaResponse generarReportePorIdentificacion(
            String identificacion, LocalDate fechaInicio, LocalDate fechaFin) {
        ClientDto cliente = clientGrpcClientService.getClientByIdentificacion(identificacion);
        if (!cliente.encontrado()) {
            throw new ResourceNotFoundException("Cliente no encontrado con identificacion: " + identificacion);
        }
        return generarReporte(cliente.idCliente(), fechaInicio, fechaFin);
    }
}
