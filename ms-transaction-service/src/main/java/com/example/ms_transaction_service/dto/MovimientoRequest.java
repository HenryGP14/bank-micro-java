package com.example.ms_transaction_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoRequest {

    private LocalDateTime fecha;

    @NotBlank
    private String numeroCuenta;

    @NotBlank
    private String tipoMovimiento;

    @NotNull
    private BigDecimal valor;
}
