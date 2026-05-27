-- Agregar columna role a users
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Tabla de mensajes internos
CREATE TABLE mensajes (
    id UUID PRIMARY KEY,
    emisor_id UUID NOT NULL REFERENCES users(id),
    receptor_id UUID NOT NULL REFERENCES users(id),
    asunto VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensajes_receptor ON mensajes(receptor_id);
CREATE INDEX idx_mensajes_emisor ON mensajes(emisor_id);
