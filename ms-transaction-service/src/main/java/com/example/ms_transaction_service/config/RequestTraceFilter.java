package com.example.ms_transaction_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Genera un UUID de correlacion por peticion REST, lo expone en MDC (visible en todos los logs
 * de la peticion, incluida la eventual llamada gRPC saliente hacia ms-client-service) y lo
 * devuelve en el header X-Request-Id para que el cliente tambien pueda correlacionarlo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String MDC_TRACE_ID = "traceId";

    /**
     * Corre primero en la cadena de filtros para que el traceId ya exista en MDC antes de
     * que cualquier otro componente empiece a loguear esta peticion.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            log.info("REST IN [{}] {} {}", traceId, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            log.info(
                    "REST OUT [{}] {} {} -> {}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
