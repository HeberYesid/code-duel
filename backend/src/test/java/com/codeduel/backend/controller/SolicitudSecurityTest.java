package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.TipoSolicitud;
import com.codeduel.backend.repository.SolicitudRepository;
import com.codeduel.backend.repository.ProfileRepository;
import com.codeduel.backend.repository.UserRepository;
import com.codeduel.backend.repository.ScoreEntryRepository;
import com.codeduel.backend.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SolicitudSecurityTest Integration Tests")
class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ScoreEntryRepository scoreEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setup() {
        solicitudRepository.deleteAll();
        notificationRepository.deleteAll();
        scoreEntryRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/solicitudes sin ninguna autenticación — HTTP 401 o 403")
    void createSolicitud_Anonymous_ShouldReturnUnauthorizedOrForbidden() throws Exception {
        SolicitudRequest request = new SolicitudRequest(TipoSolicitud.SOPORTE, "Ayuda técnica");

        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "normaluser", roles = "USER")
    @DisplayName("POST /api/solicitudes con usuario autenticado (USER) — HTTP 201 Created")
    void createSolicitud_Authenticated_ShouldReturn201() throws Exception {
        User user = User.builder()
                .username("normaluser")
                .email("normaluser@test.com")
                .passwordHash("password_hash")
                .role("USER")
                .build();
        userRepository.save(user);

        SolicitudRequest request = new SolicitudRequest(TipoSolicitud.SOPORTE, "Ayuda con acceso");

        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "regularuser", roles = "USER")
    @DisplayName("PUT /api/solicitudes/{id}/aprobar con rol USER — HTTP 403 Forbidden")
    void approveSolicitud_AsUser_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/9999/aprobar")
                        .with(csrf())
                        .param("observacion", "Aprobación inválida")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
