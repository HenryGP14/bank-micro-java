package com.example.ms_client_service.service;

import com.example.ms_client_service.dto.ClienteRequest;
import com.example.ms_client_service.dto.ClienteResponse;
import com.example.ms_client_service.dto.LoginRequest;
import com.example.ms_client_service.dto.LoginResponse;
import com.example.ms_client_service.dto.PageResponse;
import com.example.ms_client_service.entity.Cliente;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

/*
 * Reglas de negocio de cliente: alta, edicion, baja logica y login. La implementacion
 * (ClienteServiceImpl) es la que decide que la baja no borra la fila, que el update completo
 * conserva la contrasenia si no viene una nueva, etc. -- esta interfaz solo expone el contrato.
 */
public interface ClienteService {

    PageResponse<ClienteResponse> findAll(Pageable pageable);

    ClienteResponse findById(Long id);

    ClienteResponse create(ClienteRequest request);

    ClienteResponse update(Long id, ClienteRequest request);

    // igual que update, pero solo pisa los campos que vienen informados en el request
    ClienteResponse partialUpdate(Long id, ClienteRequest request);

    // baja logica: marca estado=false, no borra la fila (cuentas y movimientos en el otro
    // microservicio referencian este id)
    void delete(Long id);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    // entidad cruda (no el DTO publico), para los services internos que necesitan el objeto
    // persistente completo -- por ejemplo para validarlo via gRPC desde ms-transaction-service
    Cliente getCliente(Long id);
}
