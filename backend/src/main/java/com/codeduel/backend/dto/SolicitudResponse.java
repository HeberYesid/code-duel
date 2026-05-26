package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.EstadoSolicitud;
import com.codeduel.backend.model.enums.TipoSolicitud;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudResponse {
    private UUID id;
    private String solicitante;
    private TipoSolicitud tipoSolicitud;
    private String descripcion;
    private EstadoSolicitud estado;
    private String observacion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
