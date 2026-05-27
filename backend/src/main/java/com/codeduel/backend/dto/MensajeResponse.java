package com.codeduel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeResponse {
    private UUID id;
    private String emisor;
    private String receptor;
    private String asunto;
    private String contenido;
    private Boolean leido;
    private LocalDateTime fechaEnvio;
}
