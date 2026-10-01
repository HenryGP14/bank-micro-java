package com.example.ms_client_service.entity;

import com.example.ms_client_service.config.AesAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "persona")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @Column(nullable = false)
    private String nombre;

    private String genero;

    private Integer edad;

    @Convert(converter = AesAttributeConverter.class)
    @Column(nullable = false, unique = true)
    private String identificacion;

    @Column(name = "identificacion_hash", nullable = false, unique = true)
    private String identificacionHash;

    @Convert(converter = AesAttributeConverter.class)
    private String direccion;

    @Convert(converter = AesAttributeConverter.class)
    private String telefono;

    /**
     * Recalcula el hash de la identificacion antes de cada insert/update. Se guarda aparte
     * de {@code identificacion} (que va cifrada) porque el cifrado AES/GCM usa un IV distinto
     * en cada operacion, entonces dos valores en claro iguales nunca dan el mismo texto
     * cifrado; el hash si es estable y es lo que permite buscar/validar unicidad por
     * identificacion sin descifrar toda la tabla.
     */
    @PrePersist
    @PreUpdate
    void updateIdentificacionHash() {
        if (identificacion != null) {
            this.identificacionHash = hashValue(identificacion);
        }
    }

    /**
     * Calcula el SHA-256 del valor en claro y lo devuelve en hexadecimal.
     */
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
