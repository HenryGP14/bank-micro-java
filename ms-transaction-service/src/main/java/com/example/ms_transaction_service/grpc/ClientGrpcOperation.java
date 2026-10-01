package com.example.ms_transaction_service.grpc;

/**
 * Operaciones gRPC que este microservicio puede pedirle a ms-client-service para
 * consultar o validar clientes antes de operar cuentas y movimientos.
 */
public enum ClientGrpcOperation {
    GET_CLIENT_BY_ID,
    GET_CLIENT_BY_IDENTIFICACION,
    VALIDATE_CLIENT
}
