package com.example.ms_client_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firma, valida y lee los JWT de sesion del cliente. La clave y el tiempo de expiracion salen
 * de la configuracion ({@code jwt.secret}, {@code jwt.expiration-ms}).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera el token de sesion firmado para el cliente autenticado, con expiracion segun
     * {@code jwt.expiration-ms}.
     */
    public String generateToken(Long clientId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(clientId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el id del cliente desde el subject del token ya verificado.
     */
    public Long getClientIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Comprueba firma y expiracion del token; cualquier fallo de parseo (firma invalida,
     * expirado, malformado) se traduce simplemente en false.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Devuelve la fecha de expiracion del token en la zona horaria del servidor, util para
     * guardarla junto a la sesion en {@code SesionJwt}.
     */
    public LocalDateTime getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Calcula el hash SHA-256 del token: es lo que se guarda como identificador de sesion,
     * nunca el JWT en texto plano.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible calcular el hash del token", ex);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
