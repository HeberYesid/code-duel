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
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody SolicitudRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SolicitudResponse response = solicitudService.crearSolicitud(
                userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<SolicitudResponse>> misSolicitudes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                solicitudService.getMisSolicitudes(userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> todas() {
        return ResponseEntity.ok(solicitudService.getTodasLasSolicitudes());
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudResponse> aprobar(
            @PathVariable UUID id,
            @RequestParam(name = "observacion") String observacion) {
        return ResponseEntity.ok(solicitudService.aprobar(id, observacion));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudResponse> rechazar(
            @PathVariable UUID id,
            @RequestParam(name = "observacion") String observacion) {
        return ResponseEntity.ok(solicitudService.rechazar(id, observacion));
    }
}
