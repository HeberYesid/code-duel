package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.service.MensajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @PostMapping
    public ResponseEntity<MensajeResponse> enviarMensaje(
            @Valid @RequestBody MensajeRequest request,
            Authentication authentication) {
        MensajeResponse response = mensajeService.enviarMensaje(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bandeja-entrada")
    public ResponseEntity<List<MensajeResponse>> obtenerBandejaEntrada(Authentication authentication) {
        return ResponseEntity.ok(mensajeService.obtenerBandejaEntrada(authentication.getName()));
    }

    @GetMapping("/enviados")
    public ResponseEntity<List<MensajeResponse>> obtenerMensajesEnviados(Authentication authentication) {
        return ResponseEntity.ok(mensajeService.obtenerMensajesEnviados(authentication.getName()));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<MensajeResponse> marcarComoLeido(
            @PathVariable UUID id,
            Authentication authentication) {
        MensajeResponse response = mensajeService.marcarComoLeido(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/no-leidos/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidos(Authentication authentication) {
        long count = mensajeService.contarNoLeidos(authentication.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
