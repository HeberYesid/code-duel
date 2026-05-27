package com.codeduel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeResponse {
    private Long id;
    private String emisorUsername;
    private String receptorUsername;
    private String asunto;
    private String contenido;
    private Boolean leido;
    private LocalDateTime fechaEnvio;
}
