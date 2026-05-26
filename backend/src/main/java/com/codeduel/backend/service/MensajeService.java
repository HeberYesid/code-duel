package com.codeduel.backend.service;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
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
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;

    @Transactional
    public MensajeResponse enviarMensaje(MensajeRequest request, String emisorUsername) {
        User emisor = userRepository.findByUsername(normalizeUsername(emisorUsername))
                .orElseThrow(() -> new ResourceNotFoundException("Emisor no encontrado"));

        User receptor = userRepository.findByUsername(normalizeUsername(request.getDestinatario()))
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario no encontrado: " + request.getDestinatario()));

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .asunto(request.getAsunto().trim())
                .contenido(request.getContenido().trim())
                .build();

        mensaje = mensajeRepository.save(mensaje);
        return mapToResponse(mensaje);
    }

    public List<MensajeResponse> obtenerBandejaEntrada(String username) {
        User receptor = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return mensajeRepository.findByReceptorOrderByFechaEnvioDesc(receptor).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<MensajeResponse> obtenerMensajesEnviados(String username) {
        User emisor = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return mensajeRepository.findByEmisorOrderByFechaEnvioDesc(emisor).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MensajeResponse marcarComoLeido(UUID mensajeId, String username) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado con id: " + mensajeId));

        if (!mensaje.getReceptor().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("No tienes permiso para marcar como leído este mensaje");
        }

        mensaje.setLeido(true);
        mensaje = mensajeRepository.save(mensaje);
        return mapToResponse(mensaje);
    }

    public long contarNoLeidos(String username) {
        User receptor = userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return mensajeRepository.countByReceptorAndLeidoFalse(receptor);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private MensajeResponse mapToResponse(Mensaje mensaje) {
        return MensajeResponse.builder()
                .id(mensaje.getId())
                .emisor(mensaje.getEmisor().getUsername())
                .receptor(mensaje.getReceptor().getUsername())
                .asunto(mensaje.getAsunto())
                .contenido(mensaje.getContenido())
                .leido(mensaje.isLeido())
                .fechaEnvio(mensaje.getFechaEnvio())
                .build();
    }
}
