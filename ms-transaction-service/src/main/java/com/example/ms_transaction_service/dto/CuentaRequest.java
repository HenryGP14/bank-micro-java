package com.example.ms_transaction_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CuentaRequest {

    public static final int NUMERO_CUENTA_LONGITUD = 6;

    public static final String NUMERO_CUENTA_PATTERN = "^\\d{6}$";

    @NotBlank
    private String tipoCuenta;

    @NotNull
    private BigDecimal saldoInicial;

    @NotNull
    private Boolean estado = true;

    private Boolean aleatorio = true;

    private String numeroCuenta;

    @AssertTrue(message = "Debe indicar un numero de cuenta de 6 caracteres cuando aleatorio es false")
    @Schema(hidden = true)
    @JsonIgnore
    public boolean isNumeroCuentaValidoCuandoManual() {
        if (Boolean.FALSE.equals(aleatorio)) {
            return numeroCuenta != null
                    && !numeroCuenta.isBlank()
                    && numeroCuenta.trim().matches(NUMERO_CUENTA_PATTERN);
        }
        return true;
    }
}
