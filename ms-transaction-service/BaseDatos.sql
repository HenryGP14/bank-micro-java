-- BaseDatos.sql - ms-transaction-service (PostgreSQL 16)

CREATE DATABASE transactions_db;

\c transactions_db;

CREATE TABLE IF NOT EXISTS tipo_cuenta (
    id_tipo_cuenta BIGSERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cuenta (
    id_cuenta BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(50) NOT NULL UNIQUE,
    tipo_cuenta_id BIGINT NOT NULL REFERENCES tipo_cuenta(id_tipo_cuenta),
    saldo_inicial DECIMAL(19, 2) NOT NULL DEFAULT 0,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    cliente_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS tipo_movimiento (
    id_tipo_movimiento BIGSERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS movimiento (
    id_movimiento BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    tipo_movimiento_id BIGINT NOT NULL REFERENCES tipo_movimiento(id_tipo_movimiento),
    valor DECIMAL(19, 2) NOT NULL,
    saldo DECIMAL(19, 2) NOT NULL,
    cuenta_id BIGINT NOT NULL REFERENCES cuenta(id_cuenta),
    motivo VARCHAR(500),
    movimiento_origen_id BIGINT REFERENCES movimiento(id_movimiento)
);

INSERT INTO tipo_cuenta (descripcion) VALUES ('Ahorro'), ('Ahorros'), ('Corriente')
ON CONFLICT DO NOTHING;

INSERT INTO tipo_movimiento (descripcion) VALUES ('Deposito'), ('Retiro')
ON CONFLICT DO NOTHING;

CREATE INDEX idx_cuenta_cliente ON cuenta(cliente_id);
CREATE INDEX idx_movimiento_cuenta ON movimiento(cuenta_id);
CREATE INDEX idx_movimiento_fecha ON movimiento(fecha);

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    ip_origen VARCHAR(100),
    entidad VARCHAR(100) NOT NULL,
    entidad_id BIGINT,
    accion VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha TIMESTAMP NOT NULL
);

CREATE INDEX idx_auditoria_entidad ON auditoria(entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
