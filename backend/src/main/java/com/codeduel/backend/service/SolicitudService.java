package com.codeduel.backend.service;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.exception.BadRequestException;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UserRepository userRepository;

    @Transactional
    public SolicitudResponse crearSolicitud(String username, SolicitudRequest request) {
        User solicitante = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Solicitud solicitud = Solicitud.builder()
                .solicitante(solicitante)
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        solicitud = solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    public List<SolicitudResponse> getMisSolicitudes(String username) {
        return solicitudRepository.findBySolicitanteUsernameOrderByFechaCreacionDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SolicitudResponse> getTodasLasSolicitudes() {
        return solicitudRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long contarTotal() {
        return solicitudRepository.count();
    }

    public long contarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.countByEstado(estado);
    }

    @Transactional
    public SolicitudResponse aprobar(UUID id, String observacion) {
        return resolverSolicitud(id, EstadoSolicitud.APROBADA, observacion);
    }

    @Transactional
    public SolicitudResponse rechazar(UUID id, String observacion) {
        return resolverSolicitud(id, EstadoSolicitud.RECHAZADA, observacion);
    }

    private SolicitudResponse resolverSolicitud(UUID id, EstadoSolicitud nuevoEstado, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("La solicitud ya ha sido resuelta");
        }

        if (observacion == null || observacion.trim().isEmpty()) {
            throw new BadRequestException("La observación es obligatoria para resolver la solicitud");
        }

        solicitud.setEstado(nuevoEstado);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());

        solicitud = solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    private SolicitudResponse toResponse(Solicitud solicitud) {
        return SolicitudResponse.builder()
                .id(solicitud.getId())
                .solicitante(solicitud.getSolicitante().getUsername())
                .tipo(solicitud.getTipo())
                .descripcion(solicitud.getDescripcion())
                .estado(solicitud.getEstado())
                .observacion(solicitud.getObservacion())
                .fechaCreacion(solicitud.getFechaCreacion())
                .fechaResolucion(solicitud.getFechaResolucion())
                .build();
    }
}
