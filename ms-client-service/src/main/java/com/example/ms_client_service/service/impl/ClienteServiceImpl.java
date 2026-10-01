package com.example.ms_client_service.service.impl;

import com.example.ms_client_service.config.JwtTokenProvider;
import com.example.ms_client_service.dto.ClienteRequest;
import com.example.ms_client_service.dto.ClienteResponse;
import com.example.ms_client_service.dto.LoginRequest;
import com.example.ms_client_service.dto.LoginResponse;
import com.example.ms_client_service.dto.PageResponse;
import com.example.ms_client_service.entity.Cliente;
import com.example.ms_client_service.entity.SesionJwt;
import com.example.ms_client_service.exception.BusinessException;
import com.example.ms_client_service.exception.ResourceNotFoundException;
import com.example.ms_client_service.repository.ClienteRepository;
import com.example.ms_client_service.repository.SesionJwtRepository;
import com.example.ms_client_service.service.AuditoriaService;
import com.example.ms_client_service.service.ClienteService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Cada operacion que modifica un cliente deja rastro en AuditoriaService, y la identificacion
 * se valida como unica antes de guardar en los tres casos que la tocan (alta, update completo
 * y patch) -- el constraint unique de la tabla ya la protege, pero preferimos devolver un 400
 * con mensaje claro antes que dejar reventar la excepcion de Postgres hasta el cliente.
 */
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private static final String CLIENTE_IDENTIFICACION_DUPLICADA =
            "Lo sentimos, ya existe un cliente registrado con este numero de identificacion.";

    private static final String ENTIDAD_CLIENTE = "CLIENTE";

    private final ClienteRepository clienteRepository;
    private final SesionJwtRepository sesionJwtRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> findAll(Pageable pageable) {
        return PageResponse.from(clienteRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse findById(Long id) {
        return toResponse(getCliente(id));
    }

    @Override
    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        ensureIdentificacionDisponible(request.getIdentificacion(), null);
        Cliente cliente = new Cliente();
        mapRequest(cliente, request);
        cliente.setContrasenia(passwordEncoder.encode(request.getContrasenia()));
        cliente = clienteRepository.save(cliente);
        auditoriaService.registrar(ENTIDAD_CLIENTE, cliente.getIdPersona(), "CREAR", "Creado");
        return toResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        ensureIdentificacionDisponible(request.getIdentificacion(), id);
        Cliente cliente = getCliente(id);
        mapRequest(cliente, request);
        // si no viene contrasenia en el body dejamos la actual, no la pisamos con null
        if (request.getContrasenia() != null && !request.getContrasenia().isBlank()) {
            cliente.setContrasenia(passwordEncoder.encode(request.getContrasenia()));
        }
        cliente = clienteRepository.save(cliente);
        auditoriaService.registrar(ENTIDAD_CLIENTE, cliente.getIdPersona(), "ACTUALIZAR", "Actualizado");
        return toResponse(cliente);
    }

    // Version "patch" de update: cada campo se aplica solo si vino informado, para poder
    // mandar un body parcial sin pisar el resto del cliente con nulls.
    @Override
    @Transactional
    public ClienteResponse partialUpdate(Long id, ClienteRequest request) {
        Cliente cliente = getCliente(id);
        if (request.getNombre() != null) {
            cliente.setNombre(request.getNombre());
        }
        if (request.getGenero() != null) {
            cliente.setGenero(request.getGenero());
        }
        if (request.getEdad() != null) {
            cliente.setEdad(request.getEdad());
        }
        if (request.getIdentificacion() != null) {
            ensureIdentificacionDisponible(request.getIdentificacion(), id);
            cliente.setIdentificacion(request.getIdentificacion());
        }
        if (request.getDireccion() != null) {
            cliente.setDireccion(request.getDireccion());
        }
        if (request.getTelefono() != null) {
            cliente.setTelefono(request.getTelefono());
        }
        if (request.getEstado() != null) {
            cliente.setEstado(request.getEstado());
        }
        if (request.getContrasenia() != null && !request.getContrasenia().isBlank()) {
            cliente.setContrasenia(passwordEncoder.encode(request.getContrasenia()));
        }
        cliente = clienteRepository.save(cliente);
        auditoriaService.registrar(ENTIDAD_CLIENTE, cliente.getIdPersona(), "ACTUALIZAR", "Actualizado");
        return toResponse(cliente);
    }

    // Baja logica, nunca DELETE fisico: ms-transaction-service referencia este id en cuentas y
    // movimientos por fuera de la base de datos de este servicio (via gRPC), asi que borrar la
    // fila de verdad dejaria esas referencias apuntando a nada.
    @Override
    @Transactional
    public void delete(Long id) {
        Cliente cliente = getCliente(id);
        cliente.setEstado(false);
        clienteRepository.save(cliente);
        auditoriaService.registrar(ENTIDAD_CLIENTE, id, "ELIMINAR", "Eliminado");
    }

    // Valida identificacion + contrasenia (BCrypt) y que el cliente siga activo. Si pasa, emite
    // el JWT y ademas persiste la sesion con el hash del token (no el token en claro) junto con
    // ip/fecha de emision y expiracion, para poder auditar y revocar sesiones mas adelante.
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Cliente cliente = clienteRepository.findByIdentificacion(request.getIdentificacion())
                .orElseThrow(() -> new BusinessException("Credenciales invalidas"));

        if (!cliente.isActivo() || !passwordEncoder.matches(request.getContrasenia(), cliente.getContrasenia())) {
            throw new BusinessException("Credenciales invalidas");
        }

        String token = jwtTokenProvider.generateToken(cliente.getIdPersona());
        SesionJwt sesion = new SesionJwt();
        sesion.setCliente(cliente);
        sesion.setTokenHash(jwtTokenProvider.hashToken(token));
        sesion.setFechaEmision(LocalDateTime.now());
        sesion.setFechaExpiracion(jwtTokenProvider.getExpiration(token));
        sesion.setIpOrigen(httpRequest.getRemoteAddr());
        sesionJwtRepository.save(sesion);

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .idCliente(cliente.getIdPersona())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente getCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    private void mapRequest(Cliente cliente, ClienteRequest request) {
        cliente.setNombre(request.getNombre());
        cliente.setGenero(request.getGenero());
        cliente.setEdad(request.getEdad());
        cliente.setIdentificacion(request.getIdentificacion());
        cliente.setDireccion(request.getDireccion());
        cliente.setTelefono(request.getTelefono());
        cliente.setEstado(request.getEstado());
    }

    private void ensureIdentificacionDisponible(String identificacion, Long clienteIdExcluido) {
        // al editar excluimos el propio id, si no chocaria contra si mismo
        clienteRepository.findByIdentificacion(identificacion)
                .filter(cliente -> clienteIdExcluido == null || !cliente.getIdPersona().equals(clienteIdExcluido))
                .ifPresent(cliente -> {
                    throw new BusinessException(CLIENTE_IDENTIFICACION_DUPLICADA);
                });
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .idCliente(cliente.getIdPersona())
                .nombre(cliente.getNombre())
                .genero(cliente.getGenero())
                .edad(cliente.getEdad())
                .identificacion(cliente.getIdentificacion())
                .direccion(cliente.getDireccion())
                .telefono(cliente.getTelefono())
                .estado(cliente.getEstado())
                .build();
    }
}
