package com.codeduel.backend.service;

import com.codeduel.backend.dto.CountResponse;
import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Mensaje;
import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.MensajeRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;

    @Transactional
    public MensajeResponse enviarMensaje(String emisorUsername, MensajeRequest request) {
        User emisor = userRepository.findByUsername(emisorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Emisor no encontrado: " + emisorUsername));

        User receptor = userRepository.findByUsername(request.getDestinatario())
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario no encontrado: " + request.getDestinatario()));

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
        return mensajeRepository.findByReceptorUsernameOrderByFechaEnvioDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MensajeResponse> getEnviados(String username) {
        return mensajeRepository.findByEmisorUsernameOrderByFechaEnvioDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MensajeResponse marcarComoLeido(UUID mensajeId, String username) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        // Si no es el receptor, retornar 404 (como si no existiera)
        if (!mensaje.getReceptor().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Mensaje no encontrado");
        }

        mensaje.setLeido(true);
        mensaje = mensajeRepository.save(mensaje);
        return toResponse(mensaje);
    }

    public CountResponse contarNoLeidos(String username) {
        long count = mensajeRepository.countByReceptorUsernameAndLeidoFalse(username);
        return new CountResponse(count);
    }

    private MensajeResponse toResponse(Mensaje mensaje) {
        return MensajeResponse.builder()
                .id(mensaje.getId())
                .emisor(mensaje.getEmisor().getUsername())
                .receptor(mensaje.getReceptor().getUsername())
                .asunto(mensaje.getAsunto())
                .contenido(mensaje.getContenido())
                .leido(mensaje.getLeido())
                .fechaEnvio(mensaje.getFechaEnvio())
                .build();
    }
}
