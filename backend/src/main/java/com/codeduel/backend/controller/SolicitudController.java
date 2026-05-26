package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudResponse> crearSolicitud(
            @Valid @RequestBody SolicitudRequest request,
            Authentication authentication) {
        SolicitudResponse response = solicitudService.crearSolicitud(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<SolicitudResponse>> obtenerMisSolicitudes(Authentication authentication) {
        return ResponseEntity.ok(solicitudService.obtenerMisSolicitudes(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> obtenerTodasLasSolicitudes() {
        return ResponseEntity.ok(solicitudService.obtenerTodasLasSolicitudes());
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudResponse> aprobarSolicitud(
            @PathVariable UUID id,
            @RequestParam String observacion) {
        SolicitudResponse response = solicitudService.aprobarSolicitud(id, observacion);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudResponse> rechazarSolicitud(
            @PathVariable UUID id,
            @RequestParam String observacion) {
        SolicitudResponse response = solicitudService.rechazarSolicitud(id, observacion);
        return ResponseEntity.ok(response);
    }
}
