package com.example.ms_transaction_service.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReporteEstadoCuentaResponse {

    private Long clienteId;
    private String nombreCliente;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private List<CuentaResponse> cuentas;
    private List<ReporteCuentaResponse> detalleMovimientos;
}
