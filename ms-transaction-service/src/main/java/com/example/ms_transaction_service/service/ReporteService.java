package com.example.ms_transaction_service.service;

import com.example.ms_transaction_service.dto.ReporteEstadoCuentaResponse;
import java.time.LocalDate;

/**
 * Arma el reporte de estado de cuenta (cuentas + movimientos del rango) de un cliente.
 * Permite ubicar al cliente por id propio o por identificacion (via ms-client-service).
 */
public interface ReporteService {

    ReporteEstadoCuentaResponse generarReporte(Long clienteId, LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * Igual que {@link #generarReporte}, pero resuelve primero el clienteId consultando a
     * ms-client-service por identificacion.
     */
    ReporteEstadoCuentaResponse generarReportePorIdentificacion(
            String identificacion, LocalDate fechaInicio, LocalDate fechaFin);
}
