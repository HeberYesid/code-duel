CREATE TABLE solicitudes (
    id BIGSERIAL PRIMARY KEY,
    solicitante_id UUID NOT NULL REFERENCES users(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SOPORTE', 'ACCESO', 'INFORMACION')),
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    observacion TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP
);

CREATE INDEX idx_solicitudes_solicitante ON solicitudes(solicitante_id);
CREATE INDEX idx_solicitudes_estado ON solicitudes(estado);
