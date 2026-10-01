package com.example.ms_client_service.grpc.wire;

import com.example.grpc.client.ClientGrpcServiceGrpc;
import com.example.grpc.client.GrpcRequest;
import com.example.grpc.client.GrpcResponse;
import com.example.grpc.client.OperationType;
import com.example.ms_client_service.grpc.ClientGrpcOperation;
import com.example.ms_client_service.grpc.GrpcMessage;
import com.example.ms_client_service.service.GrpcOperationHandler;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Unica clase del servidor que usa tipos generados (target/) e io.grpc.
 * No editar archivos en target/generated-sources/protobuf.
 *
 * <p>Recibe el request tal cual llega por el wire, lo traduce a un {@link GrpcMessage} de
 * dominio via {@link GrpcWireAdapter} y delega la logica real a {@link GrpcOperationHandler};
 * este servidor no sabe nada de reglas de negocio, solo de traducir y responder.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
class GrpcServerNative extends ClientGrpcServiceGrpc.ClientGrpcServiceImplBase {

    private static final String MDC_TRACE_ID = "traceId";

    private final GrpcWireAdapter wireAdapter;
    private final GrpcOperationHandler operationHandler;

    /**
     * Propaga el traceId recibido (o genera uno nuevo) para correlacionar el log de esta
     * llamada con el del microservicio que la origino; cualquier excepcion se traduce a un
     * {@code Status} gRPC en vez de dejar la conexion colgada.
     */
    @Override
    public void execute(GrpcRequest wireRequest, StreamObserver<GrpcResponse> responseObserver) {
        String traceId = wireRequest.getTraceId().isBlank()
                ? UUID.randomUUID().toString()
                : wireRequest.getTraceId();
        MDC.put(MDC_TRACE_ID, traceId);
        try {
            ClientGrpcOperation operation = toOperation(wireRequest.getOperationType());
            GrpcMessage<?> message = wireAdapter.messageFromJson(operation, wireRequest.getPayloadJson());
            log.info("gRPC IN [{}] operacion={}", traceId, operation);
            Object result = operationHandler.handle(message);
            GrpcResponse wireResponse = GrpcResponse.newBuilder()
                    .setPayloadJson(wireAdapter.jsonFromBody(result))
                    .setTraceId(traceId)
                    .build();
            responseObserver.onNext(wireResponse);
            responseObserver.onCompleted();
            log.info("gRPC OUT [{}] operacion={} completada", traceId, operation);
        } catch (IllegalArgumentException ex) {
            log.warn("gRPC IN [{}] request invalido: {}", traceId, ex.getMessage());
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.error("gRPC IN [{}] error interno", traceId, ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Error interno en servicio gRPC").asRuntimeException());
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private ClientGrpcOperation toOperation(OperationType wireType) {
        return switch (wireType) {
            case GET_CLIENT_BY_ID -> ClientGrpcOperation.GET_CLIENT_BY_ID;
            case GET_CLIENT_BY_IDENTIFICACION -> ClientGrpcOperation.GET_CLIENT_BY_IDENTIFICACION;
            case VALIDATE_CLIENT -> ClientGrpcOperation.VALIDATE_CLIENT;
            default -> throw new IllegalArgumentException("Operacion gRPC no soportada: " + wireType);
        };
    }
}
