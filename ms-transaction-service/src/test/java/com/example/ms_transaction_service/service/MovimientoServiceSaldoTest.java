package com.example.ms_transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ms_transaction_service.entity.Cuenta;
import com.example.ms_transaction_service.entity.TipoCuenta;
import com.example.ms_transaction_service.entity.TipoMovimiento;
import com.example.ms_transaction_service.exception.SaldoNoDisponibleException;
import com.example.ms_transaction_service.service.impl.MovimientoServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceSaldoTest {

    @Mock
    private com.example.ms_transaction_service.repository.MovimientoRepository movimientoRepository;

    @Mock
    private com.example.ms_transaction_service.repository.TipoMovimientoRepository tipoMovimientoRepository;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private ClientGrpcClientService clientGrpcClientService;

    @InjectMocks
    private MovimientoServiceImpl movimientoService;

    private Cuenta cuenta;
    private TipoMovimiento retiro;

    @BeforeEach
    void setUp() {
        TipoCuenta tipoCuenta = new TipoCuenta();
        tipoCuenta.setDescripcion("Ahorros");

        cuenta = new Cuenta();
        cuenta.setIdCuenta(1L);
        cuenta.setNumeroCuenta("495878");
        cuenta.setTipoCuenta(tipoCuenta);
        cuenta.setSaldoInicial(BigDecimal.ZERO);
        cuenta.setEstado(true);
        cuenta.setClienteId(1L);

        retiro = new TipoMovimiento();
        retiro.setDescripcion("Retiro");
    }

    @Test
    void registrarRetiroSinSaldoLanzaExcepcion() {
        var request = new com.example.ms_transaction_service.dto.MovimientoRequest();
        request.setNumeroCuenta("495878");
        request.setTipoMovimiento("Retiro");
        request.setValor(new BigDecimal("100"));

        org.mockito.Mockito.when(cuentaService.getCuentaByNumero("495878")).thenReturn(cuenta);
        org.mockito.Mockito.when(tipoMovimientoRepository.findByDescripcionIgnoreCase("Retiro"))
                .thenReturn(java.util.Optional.of(retiro));

        SaldoNoDisponibleException ex = assertThrows(
                SaldoNoDisponibleException.class, () -> movimientoService.create(1L, request));
        assertEquals("Saldo no disponible", ex.getMessage());
    }
}
