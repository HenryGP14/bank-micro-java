package com.example.ms_transaction_service.config;

import com.example.ms_transaction_service.entity.TipoCuenta;
import com.example.ms_transaction_service.entity.TipoMovimiento;
import com.example.ms_transaction_service.repository.TipoCuentaRepository;
import com.example.ms_transaction_service.repository.TipoMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Precarga los catalogos de tipo de cuenta y tipo de movimiento al arrancar la app, para que
 * el ambiente quede usable sin tener que insertarlos a mano en cada entorno nuevo. Es
 * idempotente: si el registro ya existe por descripcion, no lo duplica.
 */
@Configuration
@RequiredArgsConstructor
public class CatalogDataLoader {

    private final TipoCuentaRepository tipoCuentaRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;

    /**
     * Se ejecuta una vez al levantar el contexto de Spring (ver {@code CommandLineRunner}) y
     * siembra los tipos de cuenta y movimiento basicos del negocio.
     */
    @Bean
    CommandLineRunner loadCatalogData() {
        return args -> {
            seedTipoCuenta("Ahorro");
            seedTipoCuenta("Ahorros");
            seedTipoCuenta("Corriente");
            seedTipoMovimiento("Deposito");
            seedTipoMovimiento("Retiro");
        };
    }

    private void seedTipoCuenta(String descripcion) {
        tipoCuentaRepository.findByDescripcionIgnoreCase(descripcion)
                .orElseGet(() -> {
                    TipoCuenta tipo = new TipoCuenta();
                    tipo.setDescripcion(descripcion);
                    return tipoCuentaRepository.save(tipo);
                });
    }

    private void seedTipoMovimiento(String descripcion) {
        tipoMovimientoRepository.findByDescripcionIgnoreCase(descripcion)
                .orElseGet(() -> {
                    TipoMovimiento tipo = new TipoMovimiento();
                    tipo.setDescripcion(descripcion);
                    return tipoMovimientoRepository.save(tipo);
                });
    }
}
