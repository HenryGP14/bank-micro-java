package com.example.ms_transaction_service.controller;

import com.example.ms_transaction_service.dto.BasicResponse;
import com.example.ms_transaction_service.dto.CuentaRequest;
import com.example.ms_transaction_service.dto.CuentaResponse;
import com.example.ms_transaction_service.dto.PageResponse;
import com.example.ms_transaction_service.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * CRUD de cuentas bancarias. El clienteId nunca viaja en la URL ni en el body de estos
 * endpoints: siempre sale del JWT (igual que en ClienteController), asi que no hay forma de
 * pedirle a la API que te muestre o modifique una cuenta de otro cliente, ni siquiera
 * adivinando el id de la cuenta -- el service revalida la propiedad en cada operacion.
 */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping
    public BasicResponse<PageResponse<CuentaResponse>> findAll(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(),
                "Listado de cuentas obtenido exitosamente",
                cuentaService.findAllByCliente(clienteId, pageable));
    }

    @GetMapping("/{id}")
    public BasicResponse<CuentaResponse> findById(Authentication authentication, @PathVariable Long id) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cuenta obtenida exitosamente", cuentaService.findByIdAndCliente(id, clienteId));
    }

    @PostMapping
    public ResponseEntity<BasicResponse<CuentaResponse>> create(
            Authentication authentication, @Valid @RequestBody CuentaRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BasicResponse.success(
                        HttpStatus.CREATED.value(),
                        "Cuenta registrada exitosamente",
                        cuentaService.create(clienteId, request)));
    }

    // reemplazo completo: tipoCuenta, saldoInicial y estado son obligatorios en el body.
    // Para tocar un solo campo sin mandar los demas esta el PATCH de abajo.
    @PutMapping("/{id}")
    public BasicResponse<CuentaResponse> update(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody CuentaRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cuenta actualizada exitosamente", cuentaService.update(id, clienteId, request));
    }

    @PatchMapping("/{id}")
    public BasicResponse<CuentaResponse> partialUpdate(
            Authentication authentication, @PathVariable Long id, @RequestBody CuentaRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(),
                "Cuenta actualizada exitosamente",
                cuentaService.partialUpdate(id, clienteId, request));
    }

    // baja logica: la cuenta queda con estado=false pero sus movimientos siguen existiendo
    @DeleteMapping("/{id}")
    public BasicResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long clienteId = (Long) authentication.getPrincipal();
        cuentaService.delete(id, clienteId);
        return BasicResponse.success(HttpStatus.OK.value(), "Cuenta eliminada exitosamente");
    }
}
