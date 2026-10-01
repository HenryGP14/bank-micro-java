package com.example.ms_transaction_service.service;

import com.example.ms_transaction_service.entity.Auditoria;
import com.example.ms_transaction_service.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Deja constancia de quien hizo que sobre cuentas y movimientos. Usuario e IP se resuelven
 * del contexto de la peticion actual, no se piden como parametro.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    /**
     * Guarda una fila de auditoria para la accion indicada, completando usuario/IP en
     * automatico desde el contexto de seguridad y de la request HTTP en curso.
     */
    @Transactional
    public void registrar(String entidad, Long entidadId, String accion, String estado) {
        Auditoria auditoria = new Auditoria();
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setAccion(accion);
        auditoria.setEstado(estado);
        auditoria.setUsuarioId(resolveUsuarioId());
        auditoria.setIpOrigen(resolveIp());
        auditoria.setFecha(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }

    /**
     * Toma el id de cliente desde el principal ya autenticado por el filtro JWT.
     */
    private Long resolveUsuarioId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long clienteId) {
            return clienteId;
        }
        return null;
    }

    /**
     * Recupera la IP remota del request HTTP activo via {@code RequestContextHolder}.
     */
    private String resolveIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
