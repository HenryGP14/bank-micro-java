package com.example.ms_transaction_service.controller;

import com.example.ms_transaction_service.dto.BasicResponse;
import com.example.ms_transaction_service.dto.MovimientoMotivoRequest;
import com.example.ms_transaction_service.dto.MovimientoRequest;
import com.example.ms_transaction_service.dto.MovimientoResponse;
import com.example.ms_transaction_service.dto.MovimientoReversionRequest;
import com.example.ms_transaction_service.dto.PageResponse;
import com.example.ms_transaction_service.service.MovimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Deposito, retiro, consulta y "reversion" de movimientos. No hay un PUT que reemplace un
 * movimiento completo a proposito: un movimiento ya aplicado a un saldo no se deberia poder
 * reescribir libremente sin dejar rastro, asi que las unicas formas de tocar uno existente son
 * el PATCH de motivo (metadata, no afecta el saldo) y el DELETE que lo revierte generando un
 * movimiento inverso.
 */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    @GetMapping
    public BasicResponse<PageResponse<MovimientoResponse>> findByNumeroCuenta(
            Authentication authentication,
            @RequestParam String numeroCuenta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        // paginado y ordenado por fecha desc: una cuenta con anios de historial no deberia
        // resolverse trayendo todos los movimientos de un solo tiro
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(),
                "Listado de movimientos obtenido exitosamente",
                movimientoService.findByNumeroCuenta(numeroCuenta, clienteId, pageable));
    }

    @PostMapping
    public ResponseEntity<BasicResponse<MovimientoResponse>> create(
            Authentication authentication, @Valid @RequestBody MovimientoRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BasicResponse.success(
                        HttpStatus.CREATED.value(),
                        "Movimiento registrado exitosamente",
                        movimientoService.create(clienteId, request)));
    }

    // unico campo editable de un movimiento ya creado: el motivo/descripcion. Valor y saldo
    // quedan fuera a proposito para no romper la trazabilidad contable del historial.
    @PatchMapping("/{id}")
    public BasicResponse<MovimientoResponse> actualizarMotivo(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody MovimientoMotivoRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(),
                "Movimiento actualizado exitosamente",
                movimientoService.actualizarMotivo(id, clienteId, request.getMotivo()));
    }

    // no borra el movimiento original: el service genera uno nuevo en sentido contrario para
    // que el saldo quede correcto y el historial siga siendo auditable (ver MovimientoServiceImpl)
    @DeleteMapping("/{id}")
    public BasicResponse<Void> revertir(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody MovimientoReversionRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        movimientoService.revertir(id, clienteId, request.getMotivo());
        return BasicResponse.success(HttpStatus.OK.value(), "Movimiento revertido exitosamente");
    }
}
