package com.example.ms_client_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClienteResponse {

    private Long idCliente;
    private String nombre;
    private String genero;
    private Integer edad;
    private String identificacion;
    private String direccion;
    private String telefono;
    private Boolean estado;
}
