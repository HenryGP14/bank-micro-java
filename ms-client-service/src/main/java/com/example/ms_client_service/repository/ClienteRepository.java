package com.example.ms_client_service.repository;

import com.example.ms_client_service.entity.Cliente;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    default Optional<Cliente> findByIdentificacion(String identificacion) {
        return findByIdentificacionHash(hashValue(identificacion));
    }

    Optional<Cliente> findByIdentificacionHash(String identificacionHash);

    private static String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible calcular hash de identificacion", ex);
        }
    }
}
