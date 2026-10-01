package com.example.ms_transaction_service.service.impl;

import com.example.ms_transaction_service.grpc.ClientGrpcOperation;
import com.example.ms_transaction_service.grpc.dto.ClientDto;
import com.example.ms_transaction_service.grpc.dto.GetClientByIdDto;
import com.example.ms_transaction_service.grpc.dto.GetClientByIdentificacionDto;
import com.example.ms_transaction_service.grpc.dto.ValidateClientDto;
import com.example.ms_transaction_service.grpc.dto.ValidateClientResultDto;
import com.example.ms_transaction_service.grpc.wire.ClientGrpcClientEndpoint;
import com.example.ms_transaction_service.service.ClientGrpcClientService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementacion de {@link ClientGrpcClientService}: envia la llamada gRPC de forma asincrona
 * y espera la respuesta con un timeout fijo de {@value #TIMEOUT_SECONDS} segundos. Si
 * ms-client-service no responde a tiempo o falla, la llamada no se cuelga: se traduce en
 * excepcion para que el flujo de negocio la maneje.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientGrpcClientServiceImpl implements ClientGrpcClientService {

    private static final long TIMEOUT_SECONDS = 5;

    private final ClientGrpcClientEndpoint grpcClient;

    @Override
    public ClientDto getClientById(Long clientId) {
        try {
            return grpcClient
                    .<ClientDto>sendAsync(ClientGrpcOperation.GET_CLIENT_BY_ID, new GetClientByIdDto(clientId))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.error("Error consultando cliente id={}", clientId, ex);
            throw new IllegalStateException("No fue posible validar el cliente", ex);
        }
    }

    @Override
    public ClientDto getClientByIdentificacion(String identificacion) {
        try {
            return grpcClient
                    .<ClientDto>sendAsync(
                            ClientGrpcOperation.GET_CLIENT_BY_IDENTIFICACION,
                            new GetClientByIdentificacionDto(identificacion))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.error("Error consultando cliente identificacion={}", identificacion, ex);
            throw new IllegalStateException("No fue posible validar el cliente", ex);
        }
    }

    /**
     * A diferencia de las consultas de arriba, aqui la respuesta "cliente invalido" no es un
     * error de infraestructura sino una regla de negocio: se propaga como
     * {@code IllegalArgumentException} sin envolver, para que el caller distinga "cliente
     * invalido" de "no se pudo consultar".
     */
    @Override
    public void validateClientOrThrow(Long clientId) {
        try {
            ValidateClientResultDto result = grpcClient
                    .<ValidateClientResultDto>sendAsync(
                            ClientGrpcOperation.VALIDATE_CLIENT, new ValidateClientDto(clientId))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!result.valido()) {
                throw new IllegalArgumentException(result.mensaje());
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error validando cliente id={}", clientId, ex);
            throw new IllegalStateException("No fue posible validar el cliente", ex);
        }
    }
}
