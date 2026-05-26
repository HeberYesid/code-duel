package com.codeduel.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeResponse {
    private UUID id;
    private String emisor;
    private String receptor;
    private String asunto;
    private String contenido;
    private boolean leido;
    private LocalDateTime fechaEnvio;
}
