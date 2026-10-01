package com.example.ms_client_service.controller;

import com.example.ms_client_service.dto.BasicResponse;
import com.example.ms_client_service.dto.ClienteRequest;
import com.example.ms_client_service.dto.ClienteResponse;
import com.example.ms_client_service.dto.LoginRequest;
import com.example.ms_client_service.dto.LoginResponse;
import com.example.ms_client_service.dto.PageResponse;
import com.example.ms_client_service.service.ClienteService;
import jakarta.servlet.http.HttpServletRequest;
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
 * CRUD de clientes + login. Ojo con los endpoints "/me": a proposito no reciben un id por
 * parametro. El id sale del JWT que ya valido JwtAuthenticationFilter y queda como principal
 * de la Authentication, asi que un cliente autenticado solo puede leer o modificar su propio
 * registro aunque intente mandar otro id en la URL (ni siquiera existe esa URL para el).
 * Los unicos publicos son el alta, el login y los listados de /clientes.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping("/clientes")
    public BasicResponse<PageResponse<ClienteResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return BasicResponse.success(
                HttpStatus.OK.value(), "Listado de clientes obtenido exitosamente", clienteService.findAll(pageable));
    }

    @GetMapping("/clientes/{id}")
    public BasicResponse<ClienteResponse> findById(@PathVariable Long id) {
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cliente obtenido exitosamente", clienteService.findById(id));
    }

    // version "/me": mismo findById pero resolviendo el id desde el token en vez del path
    @GetMapping("/clientes/me")
    public BasicResponse<ClienteResponse> findAuthenticated(Authentication authentication) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cliente obtenido exitosamente", clienteService.findById(clienteId));
    }

    // unico POST del recurso que no exige Authorization: todavia no existe sesion en este punto
    @PostMapping("/clientes")
    public ResponseEntity<BasicResponse<ClienteResponse>> create(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BasicResponse.success(
                        HttpStatus.CREATED.value(), "Cliente registrado exitosamente", clienteService.create(request)));
    }

    @PutMapping("/clientes/me")
    public BasicResponse<ClienteResponse> update(Authentication authentication, @Valid @RequestBody ClienteRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cliente actualizado exitosamente", clienteService.update(clienteId, request));
    }

    // a diferencia del PUT de arriba, este acepta un body incompleto: el service solo pisa
    // los campos que vienen informados y deja el resto del cliente tal cual estaba
    @PatchMapping("/clientes/me")
    public BasicResponse<ClienteResponse> partialUpdate(Authentication authentication, @RequestBody ClienteRequest request) {
        Long clienteId = (Long) authentication.getPrincipal();
        return BasicResponse.success(
                HttpStatus.OK.value(), "Cliente actualizado exitosamente", clienteService.partialUpdate(clienteId, request));
    }

    // baja logica (el service marca estado=false), no un DELETE fisico de la fila
    @DeleteMapping("/clientes/me")
    public BasicResponse<Void> delete(Authentication authentication) {
        Long clienteId = (Long) authentication.getPrincipal();
        clienteService.delete(clienteId);
        return BasicResponse.success(HttpStatus.OK.value(), "Cliente eliminado exitosamente");
    }

    @PostMapping("/auth/login")
    public BasicResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return BasicResponse.success(
                HttpStatus.OK.value(), "Inicio de sesion exitoso", clienteService.login(request, httpRequest));
    }
}
