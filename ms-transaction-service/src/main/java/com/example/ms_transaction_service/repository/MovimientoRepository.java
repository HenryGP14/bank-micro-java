package com.example.ms_transaction_service.repository;

import com.example.ms_transaction_service.entity.Movimiento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    @Query("""
            SELECT m FROM Movimiento m
            JOIN FETCH m.cuenta c
            JOIN FETCH m.tipoMovimiento
            WHERE c.clienteId = :clienteId
              AND m.fecha BETWEEN :fechaInicio AND :fechaFin
            ORDER BY m.fecha DESC
            """)
    List<Movimiento> findByClienteAndFechaBetween(
            @Param("clienteId") Long clienteId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    Page<Movimiento> findByCuenta_NumeroCuenta(String numeroCuenta, Pageable pageable);

    Optional<Movimiento> findByMovimientoOrigenId(Long movimientoOrigenId);
}
