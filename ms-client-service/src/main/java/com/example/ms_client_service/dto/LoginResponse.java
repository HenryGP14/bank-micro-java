package com.example.ms_client_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private String tipo;
    private Long idCliente;
}
