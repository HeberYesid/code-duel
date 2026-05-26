package com.codeduel.backend.repository;

import com.codeduel.backend.model.Solicitud;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {
    List<Solicitud> findBySolicitanteOrderByFechaCreacionDesc(User solicitante);
    List<Solicitud> findAllByOrderByFechaCreacionDesc();
    long countByEstado(EstadoSolicitud estado);
}
