package com.example.ms_transaction_service.config;

import com.example.ms_transaction_service.dto.BasicResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rate limiting general (token bucket con Bucket4j, en memoria) para proteger el servicio de
 * abuso o rafagas tipo DDoS. Aplica a TODAS las rutas REST con dos niveles:
 * <ul>
 *   <li>Por IP: cada IP remota tiene su propia cuota, para que un solo origen no acapare el
 *       servicio.</li>
 *   <li>Global: un techo total del microservicio, para resistir ataques distribuidos desde
 *       muchas IPs distintas.</li>
 * </ul>
 * Corre justo despues de {@link RequestTraceFilter} y antes de Spring Security, de modo que
 * las peticiones rechazadas no llegan a validar JWT ni a tocar la base de datos. Al superar el
 * limite responde 429 con el sobre {@link BasicResponse} y el header Retry-After.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String MENSAJE = "Demasiadas peticiones. Intente nuevamente mas tarde.";

    private final boolean enabled;
    private final Bandwidth perIpLimit;
    private final Bucket globalBucket;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** Buckets por IP; expiran si no se usan y el tamano esta acotado para que un ataque con
     *  muchas IPs distintas no agote la memoria. */
    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(
            @Value("${rate-limit.enabled:true}") boolean enabled,
            @Value("${rate-limit.per-ip.capacity:100}") long perIpCapacity,
            @Value("${rate-limit.per-ip.refill-period:1m}") Duration perIpRefillPeriod,
            @Value("${rate-limit.global.capacity:2000}") long globalCapacity,
            @Value("${rate-limit.global.refill-period:1m}") Duration globalRefillPeriod) {
        this.enabled = enabled;
        this.perIpLimit = Bandwidth.builder()
                .capacity(perIpCapacity)
                .refillGreedy(perIpCapacity, perIpRefillPeriod)
                .build();
        this.globalBucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(globalCapacity)
                        .refillGreedy(globalCapacity, globalRefillPeriod)
                        .build())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        Bucket ipBucket = ipBuckets.get(ip, k -> Bucket.builder().addLimit(perIpLimit).build());

        // Primero la cuota por IP: si un solo origen abusa, se le corta sin gastar cuota global.
        ConsumptionProbe ipProbe = ipBucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", String.valueOf(perIpLimit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(ipProbe.getRemainingTokens()));
        if (!ipProbe.isConsumed()) {
            reject(request, response, "ip " + ip, ipProbe.getNanosToWaitForRefill());
            return;
        }

        ConsumptionProbe globalProbe = globalBucket.tryConsumeAndReturnRemaining(1);
        if (!globalProbe.isConsumed()) {
            reject(request, response, "global", globalProbe.getNanosToWaitForRefill());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request, HttpServletResponse response, String scope, long nanosToWait)
            throws IOException {
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToWait));
        log.warn("Rate limit {} excedido en {} {}", scope, request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(jsonMapper.writeValueAsString(
                        BasicResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), MENSAJE)));
    }
}
