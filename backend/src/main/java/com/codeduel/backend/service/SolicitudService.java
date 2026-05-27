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

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UserRepository userRepository;

    @Transactional
    public SolicitudResponse crearSolicitud(String username, SolicitudRequest request) {
        User solicitante = findUser(username);

        Solicitud solicitud = Solicitud.builder()
                .solicitante(solicitante)
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .build();

        solicitud = solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    public List<SolicitudResponse> getMisSolicitudes(String username) {
        User solicitante = findUser(username);
        return solicitudRepository.findBySolicitanteOrderByFechaCreacionDesc(solicitante)
                .stream().map(this::toResponse).toList();
    }

    public List<SolicitudResponse> getTodasSolicitudes() {
        return solicitudRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SolicitudResponse aprobarSolicitud(Long id, String observacion) {
        return resolverSolicitud(id, EstadoSolicitud.APROBADA, observacion);
    }

    @Transactional
    public SolicitudResponse rechazarSolicitud(Long id, String observacion) {
        return resolverSolicitud(id, EstadoSolicitud.RECHAZADA, observacion);
    }

    private SolicitudResponse resolverSolicitud(Long id, EstadoSolicitud nuevoEstado, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException(
                    "La solicitud ya fue resuelta con estado: " + solicitud.getEstado());
        }

        solicitud.setEstado(nuevoEstado);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());

        solicitud = solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    public long contarTotal() {
        return solicitudRepository.count();
    }

    public long contarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.countByEstado(estado);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private SolicitudResponse toResponse(Solicitud s) {
        return SolicitudResponse.builder()
                .id(s.getId())
                .solicitanteUsername(s.getSolicitante().getUsername())
                .tipo(s.getTipo())
                .descripcion(s.getDescripcion())
                .estado(s.getEstado())
                .observacion(s.getObservacion())
                .fechaCreacion(s.getFechaCreacion())
                .fechaResolucion(s.getFechaResolucion())
                .build();
    }
}
