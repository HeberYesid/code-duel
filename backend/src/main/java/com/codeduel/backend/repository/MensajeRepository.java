package com.codeduel.backend.repository;

import com.codeduel.backend.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    List<Mensaje> findByReceptorUsernameOrderByFechaEnvioDesc(String username);
    List<Mensaje> findByEmisorUsernameOrderByFechaEnvioDesc(String username);
    long countByReceptorUsernameAndLeidoFalse(String username);
}
