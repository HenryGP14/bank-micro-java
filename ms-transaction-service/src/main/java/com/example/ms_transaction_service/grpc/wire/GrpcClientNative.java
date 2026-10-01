package com.example.ms_transaction_service.grpc.wire;

import com.example.grpc.client.ClientGrpcServiceGrpc;
import com.example.grpc.client.GrpcRequest;
import com.example.grpc.client.GrpcResponse;
import com.example.grpc.client.OperationType;
import com.example.ms_transaction_service.grpc.ClientGrpcOperation;
import com.example.ms_transaction_service.grpc.GrpcMessage;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

/**
 * Unica clase del cliente que usa tipos generados (target/) e io.grpc.
 * No editar archivos en target/generated-sources/protobuf.
 *
 * <p>Abre el canal hacia "client-service" una sola vez (al construirse) y lo reutiliza para
 * todas las llamadas; cada envio reusa el traceId de MDC si ya existe uno (por ejemplo, el de
 * la peticion REST que disparo la llamada), para que el log de ambos microservicios se pueda
 * correlacionar.
 */
@Component
@Slf4j
class GrpcClientNative {

    private static final String MDC_TRACE_ID = "traceId";

    private final GrpcWireAdapter wireAdapter;
    private final ClientGrpcServiceGrpc.ClientGrpcServiceFutureStub clientStub;

    GrpcClientNative(GrpcWireAdapter wireAdapter, GrpcChannelFactory channels) {
        this.wireAdapter = wireAdapter;
        this.clientStub = ClientGrpcServiceGrpc.newFutureStub(channels.createChannel("client-service"));
        log.info("Cliente gRPC conectado a client-service");
    }

    /**
     * Serializa el mensaje, lo manda por el stub gRPC y traduce el {@code ListenableFuture}
     * de Guava a un {@code CompletableFuture} de Java para que el resto del codigo no
     * dependa de la libreria de gRPC.
     */
    <T> CompletableFuture<T> send(ClientGrpcOperation operation, Object requestBody) {
        String traceId = resolveTraceId();
        log.info("gRPC OUT [{}] operacion={}", traceId, operation);
        GrpcMessage<Object> message = GrpcMessage.of(operation, requestBody);
        GrpcRequest wireRequest = GrpcRequest.newBuilder()
                .setOperationType(toWireType(operation))
                .setPayloadJson(wireAdapter.jsonFromMessage(message))
                .setTraceId(traceId)
                .build();
        ListenableFuture<GrpcResponse> future = clientStub.execute(wireRequest);
        CompletableFuture<T> result = toCompletableFuture(future)
                .thenApply(wireResponse -> wireAdapter.bodyFromJson(operation, wireResponse.getPayloadJson()));
        result.whenComplete((value, error) -> {
            if (error != null) {
                log.error("gRPC IN [{}] fallo operacion={}", traceId, operation, error);
            } else {
                log.info("gRPC IN [{}] operacion={} completada", traceId, operation);
            }
        });
        return result;
    }

    private String resolveTraceId() {
        String traceId = MDC.get(MDC_TRACE_ID);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private OperationType toWireType(ClientGrpcOperation operation) {
        return switch (operation) {
            case GET_CLIENT_BY_ID -> OperationType.GET_CLIENT_BY_ID;
            case GET_CLIENT_BY_IDENTIFICACION -> OperationType.GET_CLIENT_BY_IDENTIFICACION;
            case VALIDATE_CLIENT -> OperationType.VALIDATE_CLIENT;
        };
    }

    private <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        listenableFuture.addListener(() -> {
            try {
                completableFuture.complete(listenableFuture.get());
            } catch (Exception ex) {
                completableFuture.completeExceptionally(ex);
            }
        }, Runnable::run);
        return completableFuture;
    }
}
