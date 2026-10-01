package com.example.ms_client_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String identificacion;

    @NotBlank
    private String contrasenia;
}
