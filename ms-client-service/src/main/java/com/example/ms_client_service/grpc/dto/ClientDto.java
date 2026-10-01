package com.example.ms_client_service.grpc.dto;

public record ClientDto(
        Long idCliente,
        String nombre,
        String identificacion,
        boolean estado,
        boolean encontrado) {}
