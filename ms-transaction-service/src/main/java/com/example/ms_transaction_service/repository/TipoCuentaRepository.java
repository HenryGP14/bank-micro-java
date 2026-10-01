package com.example.ms_transaction_service.repository;

import com.example.ms_transaction_service.entity.TipoCuenta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoCuentaRepository extends JpaRepository<TipoCuenta, Long> {

    Optional<TipoCuenta> findByDescripcionIgnoreCase(String descripcion);
}
