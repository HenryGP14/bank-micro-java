# ms-transaction-service

Microservicio de **transacciones bancarias**: cuentas, movimientos (depósitos/retiros) y reportes de estado de cuenta. Actúa como **cliente gRPC** de `ms-client-service` para validar/consultar clientes (no mantiene sus propios datos de cliente, solo el `cliente_id`).

- REST: puerto `8081`
- gRPC: cliente hacia `ms-client-service` (puerto `9090`)
- Base de datos: PostgreSQL `transactions_db`
- Documentación interactiva: `http://localhost:8081/swagger-ui/index.html`

## ¿Para qué sirve?

- Crear/listar/actualizar cuentas del **cliente autenticado** (`/api/cuentas`) — el dueño siempre se toma del JWT, nunca del body.
- Registrar movimientos (`/api/movimientos`), con reglas de banco real:
  - **Depósito es libre**: cualquier cliente autenticado puede depositar en cualquier cuenta.
  - **Retiro exige ser el dueño** de la cuenta (`403` si no lo es).
  - Listar/ver movimientos de una cuenta que no es tuya también está bloqueado (`404`, no revela que la cuenta existe).
- "Eliminar" un movimiento (`DELETE /api/movimientos/{id}`) **no borra nada físicamente**: crea un nuevo movimiento de reverso (valor invertido, mismo tipo, con el `motivo` de la reversión) y ajusta el saldo de la cuenta. El movimiento original queda intacto.
- Generar el reporte de estado de cuenta por rango de fechas, filtrando por `clienteId` o por `identificacion` (resuelto vía gRPC contra `ms-client-service`).
- Registrar en una tabla de auditoría aislada (`auditoria`) cada creación/actualización/baja/reversión (IP, usuario del token, acción, fecha).

## Estructura de carpetas

```
src/main/java/com/example/ms_transaction_service/
├── MsTransactionServiceApplication.java
├── config/
│   ├── SecurityConfig.java           # Reglas de autorizacion por endpoint
│   ├── JwtAuthenticationFilter.java  # Valida el Bearer token (mismo JWT_SECRET que ms-client-service)
│   ├── RestAuthenticationEntryPoint.java # 401 con BasicResponse si el token falta/expiro/es invalido
│   ├── RequestTraceFilter.java       # X-Request-Id para correlacionar logs entre ambos MS
│   ├── CatalogDataLoader.java        # Precarga tipo_cuenta (Ahorro/Corriente) y tipo_movimiento (Deposito/Retiro)
│   └── OpenApiConfig.java
├── controller/
│   ├── CuentaController.java
│   ├── MovimientoController.java
│   └── ReporteController.java
├── dto/                               # Request/Response que viajan por REST
├── entity/
│   ├── Cuenta.java                   # estado=false = cuenta cerrada (baja logica)
│   ├── Movimiento.java               # Nunca se borra; "anulado"/"motivo" documentan reversiones
│   ├── TipoCuenta.java / TipoMovimiento.java  # Catalogos
│   └── Auditoria.java                # Tabla de auditoria aislada
├── exception/                         # Excepciones de negocio + manejador global (@RestControllerAdvice)
├── grpc/                              # Contrato interno (operaciones y DTOs) — sin tipos io.grpc aqui
│   ├── ClientGrpcOperation.java
│   ├── dto/
│   └── wire/
│       ├── GrpcClientNative.java      # Unica clase que usa tipos io.grpc/generados (cliente)
│       ├── ClientGrpcClientEndpoint.java # Fachada publica sin tipos de cable gRPC
│       └── GrpcWireAdapter.java
├── repository/                        # Spring Data JPA
└── service/
    ├── CuentaService.java / impl/
    ├── MovimientoService.java / impl/ # create() y revertir(); reglas deposito/retiro
    ├── ReporteService.java / impl/
    ├── ClientGrpcClientService.java / impl/  # Llama a ms-client-service por gRPC
    └── AuditoriaService.java
```

`src/main/proto/client_service.proto` define el contrato gRPC compartido (debe ser idéntico al de `ms-client-service`).

## Convención de la base de datos (`transactions_db`)

- Llave primaria: `id_<tabla>` (ej. `id_cuenta`).
- Llave foránea: `<tabla>_id` (ej. `cuenta_id`, `tipo_cuenta_id`).
- `cuenta.cliente_id` no tiene FK física — el cliente vive en `clients_db` (otro servicio); se valida en tiempo de ejecución vía gRPC.

Ver el diagrama completo en [`../ms-diagram.drawio.png`](../ms-diagram.drawio.png).

## Configuración (variables de entorno)

Ninguna trae valor por defecto (`application.properties` usa `${VAR}` sin fallback) — **todas son obligatorias**, si falta alguna el servicio no arranca.

| Variable | Uso |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Conexión a PostgreSQL (`DB_NAME` debe ser `transactions_db`) |
| `JWT_SECRET` | Firma de los JWT — **debe ser igual** en `ms-client-service` |
| `CLIENT_SERVICE_HOST` | Host donde escucha el servidor gRPC de `ms-client-service` (puerto `9090`) |

## Correr en local sin Docker

Requiere PostgreSQL con la base `transactions_db` (ver `BaseDatos.sql`), que `ms-client-service` esté corriendo (para el canal gRPC) y exportar las variables de entorno de arriba antes de arrancar:

```bash
cd ms-transaction-service
export DB_HOST=localhost DB_PORT=5432 DB_NAME=transactions_db DB_USER=postgres DB_PASSWORD=tu_password
export JWT_SECRET=una-clave-secreta-larga-y-aleatoria-de-al-menos-256-bits-para-hs256
export CLIENT_SERVICE_HOST=localhost
./mvnw spring-boot:run
```

## Crear una imagen Docker

```bash
# Desde la raiz de ms-transaction-service
docker build -t ms-transaction-service:latest .
```

Correrla suelta (necesita alcanzar un Postgres con `transactions_db` y el gRPC de `ms-client-service`):

```bash
docker run --rm \
  -p 8081:8081 \
  -e DB_HOST=host.docker.internal -e DB_PORT=5432 -e DB_NAME=transactions_db \
  -e DB_USER=postgres -e DB_PASSWORD=tu_password \
  -e JWT_SECRET=una-clave-secreta-larga-y-aleatoria-de-al-menos-256-bits-para-hs256 \
  -e CLIENT_SERVICE_HOST=host.docker.internal \
  ms-transaction-service:latest
```

> En la práctica es más simple construir esta imagen (paso de arriba) y usar `docker compose up -d` (o `docker stack deploy` para Swarm) desde la raíz del repo (ver el [README principal](../README.md)) — ya arma la red y conecta este servicio con `ms-client-service` por su nombre de contenedor. Postgres corre aparte, en tu host.
