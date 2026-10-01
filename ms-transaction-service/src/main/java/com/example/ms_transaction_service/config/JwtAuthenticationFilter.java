package com.example.ms_transaction_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica cada request validando el JWT emitido por ms-client-service (comparte el mismo
 * {@code jwt.secret}). A diferencia del filtro homonimo en ms-client-service, aqui no se
 * consulta ninguna tabla de sesiones: si la firma y la expiracion son validas, se confia en el
 * token; el logout/revocacion de sesion vive solo en el otro microservicio.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Si el header Bearer trae un token valido, deja autenticado al cliente para el resto
     * de la cadena de filtros; si no, sigue el flujo sin autenticacion (la ruta protegida
     * fallara mas adelante en {@code SecurityConfig}).
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.isTokenValid(token)) {
                Long clientId = jwtTokenProvider.getClientIdFromToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        clientId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
