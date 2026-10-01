package com.example.ms_client_service.service.impl;

import com.example.ms_client_service.entity.Cliente;
import com.example.ms_client_service.grpc.GrpcMessage;
import com.example.ms_client_service.grpc.dto.ClientDto;
import com.example.ms_client_service.grpc.dto.GetClientByIdDto;
import com.example.ms_client_service.grpc.dto.GetClientByIdentificacionDto;
import com.example.ms_client_service.grpc.dto.ValidateClientDto;
import com.example.ms_client_service.grpc.dto.ValidateClientResultDto;
import com.example.ms_client_service.repository.ClienteRepository;
import com.example.ms_client_service.service.GrpcOperationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resuelve cada tipo de operacion gRPC entrante contra {@code ClienteRepository}. Cuando no
 * encuentra el cliente no lanza excepcion: devuelve un DTO "vacio" con {@code encontrado=false}
 * para que el llamador (otro microservicio) lo maneje sin depender de un stacktrace gRPC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrpcOperationHandlerImpl implements GrpcOperationHandler {

    private final ClienteRepository clienteRepository;

    /**
     * Despacha segun {@code message.operation()} al metodo privado correspondiente.
     */
    @Override
    public Object handle(GrpcMessage<?> message) {
        log.info("Procesando operacion gRPC: {}", message.operation());
        return switch (message.operation()) {
            case GET_CLIENT_BY_ID -> handleGetById((GetClientByIdDto) message.body());
            case GET_CLIENT_BY_IDENTIFICACION ->
                    handleGetByIdentificacion((GetClientByIdentificacionDto) message.body());
            case VALIDATE_CLIENT -> handleValidateClient((ValidateClientDto) message.body());
        };
    }

    private ClientDto handleGetById(GetClientByIdDto payload) {
        return clienteRepository.findById(payload.clientId())
                .map(cliente -> toClientDto(cliente, true))
                .orElseGet(() -> {
                    log.warn("Cliente no encontrado por id={}", payload.clientId());
                    return new ClientDto(0L, "", "", false, false);
                });
    }

    private ClientDto handleGetByIdentificacion(GetClientByIdentificacionDto payload) {
        return clienteRepository.findByIdentificacion(payload.identificacion())
                .map(cliente -> toClientDto(cliente, true))
                .orElseGet(() -> {
                    log.warn("Cliente no encontrado por identificacion={}", payload.identificacion());
                    return new ClientDto(0L, "", "", false, false);
                });
    }

    /**
     * Un cliente solo se considera "valido" si existe y ademas esta activo; usado por
     * ms-transaction-service antes de dejar operar una cuenta.
     */
    private ValidateClientResultDto handleValidateClient(ValidateClientDto payload) {
        ValidateClientResultDto result = clienteRepository.findById(payload.clientId())
                .filter(Cliente::isActivo)
                .map(cliente -> new ValidateClientResultDto(true, "Cliente valido"))
                .orElse(new ValidateClientResultDto(false, "Cliente no encontrado o inactivo"));
        log.info("Validacion cliente id={}: valido={}", payload.clientId(), result.valido());
        return result;
    }

    private ClientDto toClientDto(Cliente cliente, boolean encontrado) {
        return new ClientDto(
                cliente.getIdPersona(),
                cliente.getNombre(),
                cliente.getIdentificacion(),
                cliente.isActivo(),
                encontrado);
    }
}
