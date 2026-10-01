package com.example.ms_transaction_service.grpc.wire;

import com.example.ms_transaction_service.grpc.ClientGrpcOperation;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fachada publica sin tipos de cable gRPC. Solo objetos Java.
 *
 * <p>Es la unica puerta que el resto del microservicio (por ejemplo
 * {@code ClientGrpcClientServiceImpl}) usa para hablar con ms-client-service; internamente
 * delega en {@link GrpcClientNative}, que si conoce los tipos generados de protobuf.
 */
@Component
@RequiredArgsConstructor
public class ClientGrpcClientEndpoint {

    private final GrpcClientNative grpcClient;

    /**
     * Envia la operacion de forma asincrona y devuelve el future que se completa cuando
     * ms-client-service responde (o falla).
     */
    public <T> CompletableFuture<T> sendAsync(ClientGrpcOperation operation, Object requestBody) {
        return grpcClient.send(operation, requestBody);
    }
}
