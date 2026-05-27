package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudResponse> crearSolicitud(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SolicitudRequest request) {
        SolicitudResponse response = solicitudService.crearSolicitud(
                userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<SolicitudResponse>> getMisSolicitudes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                solicitudService.getMisSolicitudes(userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> getTodasSolicitudes() {
        return ResponseEntity.ok(solicitudService.getTodasSolicitudes());
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudResponse> aprobarSolicitud(
            @PathVariable Long id,
            @RequestParam String observacion) {
        return ResponseEntity.ok(solicitudService.aprobarSolicitud(id, observacion));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudResponse> rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam String observacion) {
        return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, observacion));
    }
}
