package com.example.ms_client_service.repository;

import com.example.ms_client_service.entity.SesionJwt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionJwtRepository extends JpaRepository<SesionJwt, Long> {

    Optional<SesionJwt> findByTokenHashAndRevocadoFalse(String tokenHash);
}
