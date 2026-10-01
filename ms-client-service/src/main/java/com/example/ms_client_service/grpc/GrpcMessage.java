package com.example.ms_client_service.grpc;

/**
 * Sobre interno (independiente de protobuf) que agrupa que operacion se pidio y su payload ya
 * deserializado a un DTO Java. Es lo que circula entre {@code GrpcWireAdapter} y
 * {@code GrpcOperationHandler}, sin que ninguno de los dos conozca tipos de io.grpc.
 */
public record GrpcMessage<T>(ClientGrpcOperation operation, T body) {

    public static <T> GrpcMessage<T> of(ClientGrpcOperation operation, T body) {
        return new GrpcMessage<>(operation, body);
    }
}
