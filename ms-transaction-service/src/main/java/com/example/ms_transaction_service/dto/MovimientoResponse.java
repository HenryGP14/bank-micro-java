package com.example.ms_transaction_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovimientoResponse {

    private Long idMovimiento;
    private LocalDateTime fecha;
    private String numeroCuenta;
    private String tipoMovimiento;
    private BigDecimal valor;
    private BigDecimal saldo;
    private String motivo;
}
