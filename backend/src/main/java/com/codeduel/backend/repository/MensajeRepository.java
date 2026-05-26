package com.codeduel.backend.repository;

import com.codeduel.backend.model.Mensaje;
import com.codeduel.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    List<Mensaje> findByReceptorOrderByFechaEnvioDesc(User receptor);
    List<Mensaje> findByEmisorOrderByFechaEnvioDesc(User emisor);
    long countByReceptorAndLeidoFalse(User receptor);
}
