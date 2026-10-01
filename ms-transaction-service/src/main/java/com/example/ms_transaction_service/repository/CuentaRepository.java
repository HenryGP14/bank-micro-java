package com.example.ms_transaction_service.repository;

import com.example.ms_transaction_service.entity.Cuenta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteId(Long clienteId);

    Page<Cuenta> findByClienteId(Long clienteId, Pageable pageable);
}
