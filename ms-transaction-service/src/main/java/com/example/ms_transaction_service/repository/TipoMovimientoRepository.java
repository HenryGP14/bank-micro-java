package com.example.ms_transaction_service.repository;

import com.example.ms_transaction_service.entity.TipoMovimiento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {

    Optional<TipoMovimiento> findByDescripcionIgnoreCase(String descripcion);
}
