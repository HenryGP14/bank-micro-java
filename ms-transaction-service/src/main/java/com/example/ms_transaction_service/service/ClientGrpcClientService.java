package com.example.ms_transaction_service.service;

import com.example.ms_transaction_service.grpc.dto.ClientDto;

/**
 * Puerta de salida hacia ms-client-service via gRPC. Todo lo que este microservicio necesita
 * saber de un cliente (existencia, datos, si esta activo) pasa por aqui, nunca por acceso
 * directo a la base de datos de clientes.
 */
public interface ClientGrpcClientService {

    ClientDto getClientById(Long clientId);

    ClientDto getClientByIdentificacion(String identificacion);

    /**
     * Lanza una excepcion de negocio si el cliente no existe o esta inactivo; se usa como
     * guardia antes de operaciones sensibles (por ejemplo, abrir una cuenta).
     */
    void validateClientOrThrow(Long clientId);
}
