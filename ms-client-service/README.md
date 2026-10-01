# ms-client-service

Microservicio de **clientes**: alta, autenticación (login con JWT) y gestión del catálogo de clientes del banco. Expone además un **servidor gRPC** para que `ms-transaction-service` pueda validar/consultar clientes sin exponer esa información por REST.

- REST: puerto `8080`
- gRPC (servidor): puerto `9090`
- Base de datos: PostgreSQL `clients_db`
- Documentación interactiva: `http://localhost:8080/swagger-ui/index.html`

## ¿Para qué sirve?

- Registrar clientes (`POST /api/clientes`, público).
- Login (`POST /api/auth/login`) — devuelve un JWT que el propio cliente usa para todo lo demás (incluyendo llamadas a `ms-transaction-service`).
- Consultar/actualizar/"eliminar" (baja lógica, `estado=false`) el cliente autenticado (`/api/clientes/me`), siempre a partir del token — nunca por un id arbitrario.
- Cifrar en reposo los datos sensibles (`identificacion`, `direccion`, `telefono`) con AES, manteniendo un hash de la identificación para poder buscarla.
- Registrar en una tabla de auditoría aislada (`auditoria`) quién hizo qué, desde qué IP y cuándo.
- Atender por gRPC las consultas que necesita `ms-transaction-service`: buscar cliente por id/identificación y validar que un cliente existe y está activo.

## Estructura de carpetas

```
src/main/java/com/example/ms_client_service/
├── MsClientServiceApplication.java   # Clase main (Spring Boot)
├── config/
│   ├── SecurityConfig.java           # Reglas de autorizacion por endpoint
│   ├── JwtAuthenticationFilter.java  # Valida el Bearer token en cada request
│   ├── JwtTokenProvider.java         # Genera/valida/firma los JWT
│   ├── RestAuthenticationEntryPoint.java # 401 con BasicResponse si el token falta/expiro/es invalido
│   ├── RequestTraceFilter.java       # Genera X-Request-Id para correlacionar logs entre ambos MS
│   ├── CryptoConfig.java / AesAttributeConverter.java # Cifrado AES de campos sensibles
│   └── OpenApiConfig.java            # Configuracion de Swagger/OpenAPI
├── controller/
│   └── ClienteController.java        # Endpoints REST: /api/clientes, /api/auth/login
├── dto/                               # Request/Response que viajan por REST
├── entity/
│   ├── Persona.java / Cliente.java   # Herencia JOINED: datos base + credenciales/estado
│   ├── SesionJwt.java                # Sesiones/tokens emitidos (permite revocar)
│   └── Auditoria.java                # Tabla de auditoria aislada
├── exception/                         # Excepciones de negocio + manejador global (@RestControllerAdvice)
├── grpc/                              # Contrato interno (operaciones y DTOs) — sin tipos io.grpc aqui
│   ├── ClientGrpcOperation.java       # Enum de operaciones soportadas
│   ├── dto/                           # DTOs planos que via a por el canal gRPC (JSON)
│   └── wire/
│       ├── GrpcServerNative.java      # Unica clase que usa tipos io.grpc/generados (servidor)
│       └── GrpcWireAdapter.java       # Serializa/deserializa el payload JSON del mensaje gRPC
├── repository/                        # Spring Data JPA
└── service/
    ├── ClienteService.java / impl/    # Logica de negocio de clientes
    ├── AuditoriaService.java          # Registra eventos de auditoria
    └── GrpcOperationHandler.java / impl/ # Resuelve cada operacion gRPC entrante
```

`src/main/proto/client_service.proto` define el contrato gRPC compartido (debe ser idéntico al de `ms-transaction-service`).

## Funcionamiento

1. **Registro**: `POST /api/clientes` es público. Valida que la `identificacion` no esté ya registrada (comparando el hash) y cifra los campos sensibles antes de guardar.
2. **Login**: `POST /api/auth/login` verifica credenciales, genera un JWT (firmado con `jwt.secret`) y guarda una fila en `sesion_jwt` (permite revocar sesiones sin esperar a que expire el token).
3. **Peticiones autenticadas**: `JwtAuthenticationFilter` valida el `Authorization: Bearer <token>` en cada request; si falta, es inválido o expiró, `RestAuthenticationEntryPoint` responde `401` con un mensaje claro dentro del mismo `BasicResponse` que usa el resto de la API.
4. **gRPC**: `ms-transaction-service` llama a este servicio (puerto `9090`) para validar un cliente antes de crear una cuenta/movimiento, o para resolver su nombre en cuentas/reportes.
5. **Auditoría**: toda creación/actualización/baja de un cliente queda registrada en la tabla `auditoria` (IP, usuario del token, acción, fecha).

## Configuración (variables de entorno)

Ninguna trae valor por defecto (`application.properties` usa `${VAR}` sin fallback) — **todas son obligatorias**, si falta alguna el servicio no arranca.

| Variable | Uso |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Conexión a PostgreSQL (`DB_NAME` debe ser `clients_db`) |
| `JWT_SECRET` | Firma de los JWT — **debe ser igual** en `ms-transaction-service` |
| `AES_SECRET` | Cifrado de campos sensibles de `persona` (32 caracteres exactos) |

## Correr en local sin Docker

Requiere una instancia de PostgreSQL accesible con la base `clients_db` (ver `BaseDatos.sql`) y exportar las variables de entorno de arriba antes de arrancar:

```bash
cd ms-client-service
export DB_HOST=localhost DB_PORT=5432 DB_NAME=clients_db DB_USER=postgres DB_PASSWORD=tu_password
export JWT_SECRET=una-clave-secreta-larga-y-aleatoria-de-al-menos-256-bits-para-hs256
export AES_SECRET=clave-aes-32-caracteres-exactos!!
./mvnw spring-boot:run
```

## Crear una imagen Docker

El `Dockerfile` es multi-stage (compila con Maven y luego corre solo el JAR):

```bash
# Desde la raiz de ms-client-service
docker build -t ms-client-service:latest .
```

Correrla suelta (necesita una red donde alcance a un Postgres con `clients_db`):

```bash
docker run --rm \
  -p 8080:8080 -p 9090:9090 \
  -e DB_HOST=host.docker.internal -e DB_PORT=5432 -e DB_NAME=clients_db \
  -e DB_USER=postgres -e DB_PASSWORD=tu_password \
  -e JWT_SECRET=una-clave-secreta-larga-y-aleatoria-de-al-menos-256-bits-para-hs256 \
  -e AES_SECRET=clave-aes-32-caracteres-exactos!! \
  ms-client-service:latest
```

> En la práctica es más simple construir esta imagen (paso de arriba) y usar `docker compose up -d` (o `docker stack deploy` para Swarm) desde la raíz del repo (ver el [README principal](../README.md)) — ya arma la red y, si usas `docker-compose.override.yml` de ejemplo, las variables de entorno correctas.
