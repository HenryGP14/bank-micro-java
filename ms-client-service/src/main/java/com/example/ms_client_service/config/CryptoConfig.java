package com.example.ms_client_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializa {@link AesAttributeConverter} con el secreto AES al arrancar el contexto de
 * Spring, para que este listo antes de que JPA lo use en el primer cifrado/descifrado.
 */
@Configuration
public class CryptoConfig {

    /**
     * Recibe el secreto desde {@code crypto.aes.secret} y lo pasa al converter estatico.
     */
    public CryptoConfig(@Value("${crypto.aes.secret}") String secret) {
        AesAttributeConverter.init(secret);
    }
}
