-- BaseDatos.sql - ms-client-service (PostgreSQL 16)

CREATE DATABASE clients_db;

\c clients_db;

CREATE TABLE IF NOT EXISTS persona (
    id_persona BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    genero VARCHAR(50),
    edad INTEGER,
    identificacion VARCHAR(500) NOT NULL UNIQUE,
    identificacion_hash VARCHAR(64) NOT NULL UNIQUE,
    direccion VARCHAR(500),
    telefono VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente BIGINT PRIMARY KEY REFERENCES persona(id_persona),
    contrasenia VARCHAR(255) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sesion_jwt (
    id_sesion_jwt BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id_cliente),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    fecha_emision TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    ip_origen VARCHAR(100)
);

CREATE INDEX idx_sesion_jwt_cliente ON sesion_jwt(cliente_id);

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
