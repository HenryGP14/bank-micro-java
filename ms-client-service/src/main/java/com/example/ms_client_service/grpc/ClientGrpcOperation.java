package com.example.ms_client_service.grpc;

/**
 * Operaciones gRPC que ms-client-service atiende para otros microservicios (hoy solo
 * ms-transaction-service, consultando o validando clientes).
 */
public enum ClientGrpcOperation {
    GET_CLIENT_BY_ID,
    GET_CLIENT_BY_IDENTIFICACION,
    VALIDATE_CLIENT
}
