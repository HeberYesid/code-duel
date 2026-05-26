package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudRequest {

    @NotNull(message = "El tipo de solicitud es obligatorio (SOPORTE, ACCESO o INFORMACIÓN)")
    private TipoSolicitud tipo;

    @NotBlank(message = "La descripción de la solicitud es obligatoria")
    private String descripcion;
}
