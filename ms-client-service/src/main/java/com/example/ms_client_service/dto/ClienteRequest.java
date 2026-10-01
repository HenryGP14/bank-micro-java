package com.example.ms_client_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteRequest {

    @NotBlank
    private String nombre;

    private String genero;

    private Integer edad;

    @NotBlank
    private String identificacion;

    private String direccion;

    private String telefono;

    @NotBlank
    private String contrasenia;

    @NotNull
    private Boolean estado = true;
}
