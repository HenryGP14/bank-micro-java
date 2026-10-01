package com.example.ms_transaction_service.grpc.wire;

import com.example.ms_transaction_service.grpc.ClientGrpcOperation;
import com.example.ms_transaction_service.grpc.GrpcMessage;
import com.example.ms_transaction_service.grpc.dto.ClientDto;
import com.example.ms_transaction_service.grpc.dto.GetClientByIdDto;
import com.example.ms_transaction_service.grpc.dto.GetClientByIdentificacionDto;
import com.example.ms_transaction_service.grpc.dto.ValidateClientDto;
import com.example.ms_transaction_service.grpc.dto.ValidateClientResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Solo objetos Java y JSON. Sin imports de io.grpc ni com.example.grpc.
 *
 * <p>Sabe que DTO de respuesta corresponde a cada {@link ClientGrpcOperation} y hace de
 * traductor JSON &lt;-&gt; objeto para las llamadas salientes hacia ms-client-service.
 */
@Component
@Slf4j
class GrpcWireAdapter {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    GrpcMessage<?> messageFromJson(ClientGrpcOperation operation, String payloadJson) {
        Object body = fromJson(payloadJson, requestType(operation));
        log.debug("Objeto recibido: operacion={}, tipo={}", operation, body.getClass().getSimpleName());
        return GrpcMessage.of(operation, body);
    }

    String jsonFromBody(Object body) {
        String json = toJson(body);
        log.debug("Objeto enviado: tipo={}, bytes={}", body.getClass().getSimpleName(), json.length());
        return json;
    }

    /**
     * Deserializa la respuesta de ms-client-service al DTO que corresponde segun la
     * operacion original (por ejemplo, {@code ClientDto} para las consultas de cliente).
     */
    <T> T bodyFromJson(ClientGrpcOperation operation, String payloadJson) {
        Class<T> type = responseType(operation);
        T body = fromJson(payloadJson, type);
        log.debug("Objeto recibido: operacion={}, tipo={}", operation, type.getSimpleName());
        return body;
    }

    String jsonFromMessage(GrpcMessage<?> message) {
        return toJson(message.body());
    }

    private Class<?> requestType(ClientGrpcOperation operation) {
        return switch (operation) {
            case GET_CLIENT_BY_ID -> GetClientByIdDto.class;
            case GET_CLIENT_BY_IDENTIFICACION -> GetClientByIdentificacionDto.class;
            case VALIDATE_CLIENT -> ValidateClientDto.class;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> responseType(ClientGrpcOperation operation) {
        return (Class<T>) switch (operation) {
            case GET_CLIENT_BY_ID, GET_CLIENT_BY_IDENTIFICACION -> ClientDto.class;
            case VALIDATE_CLIENT -> ValidateClientResultDto.class;
        };
    }

    private String toJson(Object body) {
        try {
            return jsonMapper.writeValueAsString(body);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No fue posible serializar el objeto", ex);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return jsonMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("JSON invalido para " + type.getSimpleName(), ex);
        }
    }
}
