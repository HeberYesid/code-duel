package com.codeduel.backend.service;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.dto.UnreadCountResponse;
import com.codeduel.backend.exception.BadRequestException;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Mensaje;
import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.MensajeRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;

    @Transactional
    public MensajeResponse enviarMensaje(String emisorUsername, MensajeRequest request) {
        User emisor = userRepository.findByUsername(emisorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Emisor no encontrado"));

        User receptor = userRepository.findByUsername(request.getDestinatarioUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destinatario no encontrado: " + request.getDestinatarioUsername()));

        if (emisor.getId().equals(receptor.getId())) {
            throw new BadRequestException("No puedes enviarte mensajes a ti mismo");
        }

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .asunto(request.getAsunto())
                .contenido(request.getContenido())
                .build();

        mensaje = mensajeRepository.save(mensaje);
        return toResponse(mensaje);
    }

    public List<MensajeResponse> getBandejaEntrada(String username) {
        User receptor = findUser(username);
        return mensajeRepository.findByReceptorOrderByFechaEnvioDesc(receptor)
                .stream().map(this::toResponse).toList();
    }

    public List<MensajeResponse> getEnviados(String username) {
        User emisor = findUser(username);
        return mensajeRepository.findByEmisorOrderByFechaEnvioDesc(emisor)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MensajeResponse marcarComoLeido(Long mensajeId, String username) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mensaje no encontrado con id: " + mensajeId));

        if (!mensaje.getReceptor().getUsername().equals(username)) {
            throw new AccessDeniedException("No puedes marcar como leído un mensaje que no es tuyo");
        }

        mensaje.setLeido(true);
        mensaje = mensajeRepository.save(mensaje);
        return toResponse(mensaje);
    }

    public UnreadCountResponse contarNoLeidos(String username) {
        User receptor = findUser(username);
        long count = mensajeRepository.countByReceptorAndLeidoFalse(receptor);
        return new UnreadCountResponse(count);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private MensajeResponse toResponse(Mensaje m) {
        return MensajeResponse.builder()
                .id(m.getId())
                .emisorUsername(m.getEmisor().getUsername())
                .receptorUsername(m.getReceptor().getUsername())
                .asunto(m.getAsunto())
                .contenido(m.getContenido())
                .leido(m.getLeido())
                .fechaEnvio(m.getFechaEnvio())
                .build();
    }
}
