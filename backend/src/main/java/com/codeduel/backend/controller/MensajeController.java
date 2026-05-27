package com.codeduel.backend.controller;

import com.codeduel.backend.dto.CountResponse;
import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.service.MensajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @PostMapping
    public ResponseEntity<MensajeResponse> enviarMensaje(
            @Valid @RequestBody MensajeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MensajeResponse response = mensajeService.enviarMensaje(
                userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bandeja-entrada")
    public ResponseEntity<List<MensajeResponse>> getBandejaEntrada(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.getBandejaEntrada(userDetails.getUsername()));
    }

    @GetMapping("/enviados")
    public ResponseEntity<List<MensajeResponse>> getEnviados(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.getEnviados(userDetails.getUsername()));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<MensajeResponse> marcarComoLeido(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.marcarComoLeido(id, userDetails.getUsername()));
    }

    @GetMapping("/no-leidos/count")
    public ResponseEntity<CountResponse> contarNoLeidos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.contarNoLeidos(userDetails.getUsername()));
    }
}
