package com.example.ms_transaction_service.service;

import com.example.ms_transaction_service.dto.MovimientoRequest;
import com.example.ms_transaction_service.dto.MovimientoResponse;
import com.example.ms_transaction_service.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Reglas de negocio de depositos, retiros y reversiones. Controla el saldo disponible antes
 * de descontar un retiro y que cada operacion pertenezca al cliente autenticado.
 */
public interface MovimientoService {

    /**
     * Pagina el historial de una cuenta en vez de traerlo completo: una cuenta con anos de
     * movimientos no deberia resolverse en una sola consulta JPA.
     */
    PageResponse<MovimientoResponse> findByNumeroCuenta(String numeroCuenta, Long clienteId, Pageable pageable);

    /**
     * Aplica un deposito o un retiro; un retiro que deje saldo negativo se rechaza antes de
     * tocar la base de datos.
     */
    MovimientoResponse create(Long clienteId, MovimientoRequest request);

    /**
     * Edita unicamente el motivo/descripcion del movimiento; valor y saldo quedan fuera de
     * cualquier edicion a proposito, para no romper la trazabilidad contable del historial
     * (para corregir un valor existe {@link #revertir}, que crea un movimiento nuevo en vez de
     * mutar uno existente).
     */
    MovimientoResponse actualizarMotivo(Long id, Long clienteId, String motivo);

    /**
     * Anula el movimiento devolviendo su efecto sobre el saldo de la cuenta.
     */
    void revertir(Long id, Long clienteId, String motivo);
}
