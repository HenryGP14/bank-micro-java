package com.example.ms_transaction_service.service;

import com.example.ms_transaction_service.dto.CuentaRequest;
import com.example.ms_transaction_service.dto.CuentaResponse;
import com.example.ms_transaction_service.dto.PageResponse;
import com.example.ms_transaction_service.entity.Cuenta;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Reglas de negocio de cuentas: alta, edicion, baja y consulta. La implementacion valida
 * contra ms-client-service (via gRPC) que el cliente exista y este activo antes de dejarlo
 * abrir una cuenta. Ver {@code CuentaServiceImpl}.
 */
public interface CuentaService {

    /**
     * Trae todas las cuentas del cliente sin paginar; de uso interno (reportes), que necesitan
     * el total para calcular saldos, nunca de un endpoint publico que pueda devolver una lista
     * sin limite.
     */
    List<CuentaResponse> findAllByCliente(Long clienteId);

    /**
     * Version paginada de {@link #findAllByCliente(Long)}, la que expone el controller.
     */
    PageResponse<CuentaResponse> findAllByCliente(Long clienteId, Pageable pageable);

    /**
     * Consulta puntual de una cuenta, solo si pertenece al cliente que la pide.
     */
    CuentaResponse findByIdAndCliente(Long id, Long clienteId);

    /**
     * Abre una cuenta nueva, validando primero que el cliente exista y este activo.
     */
    CuentaResponse create(Long clienteId, CuentaRequest request);

    /**
     * Reemplazo completo de una cuenta (todos los campos editables son obligatorios), solo si
     * pertenece al cliente que la pide.
     */
    CuentaResponse update(Long id, Long clienteId, CuentaRequest request);

    /**
     * Actualiza una cuenta solo si pertenece al cliente que la pide.
     */
    CuentaResponse partialUpdate(Long id, Long clienteId, CuentaRequest request);

    /**
     * Baja logica de la cuenta, tambien restringida al dueno.
     */
    void delete(Long id, Long clienteId);

    /**
     * Busca la entidad Cuenta cruda por id; la usan otros services (movimientos, reportes)
     * que necesitan el objeto persistente.
     */
    Cuenta getCuenta(Long id);

    Cuenta getCuentaByNumero(String numeroCuenta);
}
