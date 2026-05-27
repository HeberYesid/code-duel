CREATE TABLE mensajes (
    id BIGSERIAL PRIMARY KEY,
    emisor_id UUID NOT NULL REFERENCES users(id),
    receptor_id UUID NOT NULL REFERENCES users(id),
    asunto VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensajes_receptor ON mensajes(receptor_id);
CREATE INDEX idx_mensajes_emisor ON mensajes(emisor_id);
