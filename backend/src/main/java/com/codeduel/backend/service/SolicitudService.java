package com.codeduel.backend.service;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Solicitud;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.EstadoSolicitud;
import com.codeduel.backend.repository.SolicitudRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UserRepository userRepository;

    @Transactional
    public SolicitudResponse crearSolicitud(SolicitudRequest request, String username) {
        User solicitante = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Solicitud solicitud = Solicitud.builder()
                .solicitante(solicitante)
                .tipoSolicitud(request.getTipo())
                .descripcion(request.getDescripcion().trim())
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        solicitud = solicitudRepository.save(solicitud);
        return mapToResponse(solicitud);
    }

    public List<SolicitudResponse> obtenerMisSolicitudes(String username) {
        User solicitante = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return solicitudRepository.findBySolicitanteOrderByFechaCreacionDesc(solicitante).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<SolicitudResponse> obtenerTodasLasSolicitudes() {
        return solicitudRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public SolicitudResponse aprobarSolicitud(UUID id, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setObservacion(observacion != null ? observacion.trim() : "");
        solicitud.setFechaResolucion(LocalDateTime.now());

        solicitud = solicitudRepository.save(solicitud);
        return mapToResponse(solicitud);
    }

    @Transactional
    public SolicitudResponse rechazarSolicitud(UUID id, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setObservacion(observacion != null ? observacion.trim() : "");
        solicitud.setFechaResolucion(LocalDateTime.now());

        solicitud = solicitudRepository.save(solicitud);
        return mapToResponse(solicitud);
    }

    public long countTotal() {
        return solicitudRepository.count();
    }

    public long countPendientes() {
        return solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE);
    }

    public long countAprobadas() {
        return solicitudRepository.countByEstado(EstadoSolicitud.APROBADA);
    }

    public long countRechazadas() {
        return solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private SolicitudResponse mapToResponse(Solicitud solicitud) {
        return SolicitudResponse.builder()
                .id(solicitud.getId())
                .solicitante(solicitud.getSolicitante().getUsername())
                .tipoSolicitud(solicitud.getTipoSolicitud())
                .descripcion(solicitud.getDescripcion())
                .estado(solicitud.getEstado())
                .observacion(solicitud.getObservacion())
                .fechaCreacion(solicitud.getFechaCreacion())
                .fechaResolucion(solicitud.getFechaResolucion())
                .build();
    }
}
