package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.EstadoSolicitud;
import com.codeduel.backend.model.enums.TipoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudResponse {
    private Long id;
    private String solicitanteUsername;
    private TipoSolicitud tipo;
    private String descripcion;
    private EstadoSolicitud estado;
    private String observacion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
