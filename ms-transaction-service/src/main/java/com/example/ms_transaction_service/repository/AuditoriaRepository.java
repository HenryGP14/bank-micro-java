package com.example.ms_transaction_service.repository;

import com.example.ms_transaction_service.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}
