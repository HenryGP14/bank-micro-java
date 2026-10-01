package com.example.ms_client_service.config;

import com.example.ms_client_service.dto.BasicResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde con el mismo sobre BasicResponse cuando falta el token, es invalido o expiro,
 * en vez de dejar que Spring Security devuelva un 403 vacio sin explicacion.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MENSAJE = "Token invalido o expirado. Inicie sesion nuevamente.";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * Se dispara cuando Spring Security rechaza el request antes de llegar al controller;
     * arma manualmente el JSON de error porque aqui no hay un {@code @ExceptionHandler} activo.
     */
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter()
                    .write(jsonMapper.writeValueAsString(
                            BasicResponse.error(HttpStatus.UNAUTHORIZED.value(), MENSAJE)));
        } catch (Exception ignored) {
            // Si ni siquiera se puede escribir la respuesta, no hay mas que hacer aqui.
        }
    }
}
