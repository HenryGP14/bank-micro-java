package com.example.ms_transaction_service.grpc;

/**
 * Sobre interno (independiente de protobuf) que agrupa la operacion pedida y su payload ya
 * como objeto Java, para que {@code GrpcClientNative} y {@code GrpcWireAdapter} no necesiten
 * conocer tipos de io.grpc.
 */
public record GrpcMessage<T>(ClientGrpcOperation operation, T body) {

    public static <T> GrpcMessage<T> of(ClientGrpcOperation operation, T body) {
        return new GrpcMessage<>(operation, body);
    }
}
