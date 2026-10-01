package com.example.ms_transaction_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoReversionRequest {

    @NotBlank
    private String motivo;
}
