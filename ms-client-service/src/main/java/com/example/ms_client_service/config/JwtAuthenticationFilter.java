package com.example.ms_client_service.config;

import com.example.ms_client_service.repository.SesionJwtRepository;
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
 * Lee el Bearer token de cada request, valida su firma y comprueba que la sesion siga activa
 * en {@code SesionJwtRepository} (no revocada). Si todo encaja, autentica al cliente en el
 * contexto de seguridad para el resto del filtro chain.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SesionJwtRepository sesionJwtRepository;

    /**
     * Valida el token contra la sesion persistida antes de autenticar; un token con firma
     * valida pero ya revocado (logout) no otorga acceso.
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
                String tokenHash = jwtTokenProvider.hashToken(token);
                sesionJwtRepository.findByTokenHashAndRevocadoFalse(tokenHash).ifPresent(session -> {
                    Long clientId = jwtTokenProvider.getClientIdFromToken(token);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            clientId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
