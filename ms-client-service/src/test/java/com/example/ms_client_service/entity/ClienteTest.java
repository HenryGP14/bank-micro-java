package com.example.ms_client_service.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void isActivoRetornaTrueCuandoEstadoEsTrue() {
        Cliente cliente = new Cliente();
        cliente.setEstado(true);
        assertTrue(cliente.isActivo());
    }

    @Test
    void isActivoRetornaFalseCuandoEstadoEsNullOFalso() {
        Cliente cliente = new Cliente();
        cliente.setEstado(false);
        assertFalse(cliente.isActivo());
        cliente.setEstado(null);
        assertFalse(cliente.isActivo());
    }
}
