package com.example.ms_client_service.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra/descifra los campos sensibles de Persona (identificacion, direccion, telefono) antes
 * de que JPA los guarde o lea de la base de datos. Usa AES/GCM con IV aleatorio por operacion.
 */
@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static volatile SecretKeySpec secretKey;

    /**
     * Deriva la clave AES-256 a partir del secreto de {@code crypto.aes.secret}; la llama
     * {@link CryptoConfig} una sola vez al arrancar el contexto de Spring.
     */
    public static void init(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            byte[] normalized = new byte[32];
            System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
            keyBytes = normalized;
        }
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Cifra el valor antes de persistirlo; antepone el IV al texto cifrado para poder
     * descifrarlo despues.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Error al cifrar dato sensible", ex);
        }
    }

    /**
     * Descifra el valor leido de la columna: separa el IV de los primeros bytes y descifra
     * el resto con la misma clave AES.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Error al descifrar dato sensible", ex);
        }
    }

    private SecretKeySpec getSecretKey() {
        if (secretKey == null) {
            throw new IllegalStateException("Crypto no inicializado");
        }
        return secretKey;
    }
}
