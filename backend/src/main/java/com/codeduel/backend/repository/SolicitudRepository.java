package com.codeduel.backend.repository;

import com.codeduel.backend.model.Solicitud;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findBySolicitanteOrderByFechaCreacionDesc(User solicitante);

    long countByEstado(EstadoSolicitud estado);
}
