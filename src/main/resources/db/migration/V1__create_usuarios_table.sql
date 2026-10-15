CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(100) UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telefono VARCHAR(20),
    rol VARCHAR(20) DEFAULT 'USER' NOT NULL,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Datos iniciales de prueba (Seeds)
INSERT INTO usuarios (nombre, email, telefono, rol, activo)
VALUES 
    ('Administrador MediRecord', 'admin@medirecord.com', '4180000000', 'ADMIN', TRUE),
    ('Usuario Recordatorio', 'usuario@medirecord.com', '4181234567', 'USER', TRUE)
ON CONFLICT (email) DO NOTHING;
