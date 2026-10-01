package com.example.ms_client_service.service;

import com.example.ms_client_service.grpc.GrpcMessage;

/**
 * Punto de entrada unico para las operaciones gRPC que llegan de otros microservicios (hoy,
 * ms-transaction-service consultando/validando clientes). Ver
 * {@link com.example.ms_client_service.service.impl.GrpcOperationHandlerImpl} para el detalle
 * de cada operacion soportada.
 */
public interface GrpcOperationHandler {

    Object handle(GrpcMessage<?> message);
}
