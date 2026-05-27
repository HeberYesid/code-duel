package com.codeduel.backend.controller;

import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.UserRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Solicitud Security Integration Tests")
class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User testUser = User.builder()
                .username("testuser")
                .email("test@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .role("USER")
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/solicitudes sin autenticación → 401 o 403")
    void crearSolicitud_SinAutenticacion_DeberiaRetornar4xx() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "tipo", "SOPORTE",
                "descripcion", "Necesito ayuda con mi cuenta"
        ));

        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("POST /api/solicitudes con usuario autenticado → 201 Created")
    void crearSolicitud_ConUsuarioAutenticado_DeberiaRetornar201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "tipo", "SOPORTE",
                "descripcion", "Necesito soporte técnico"
        ));

        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    @DisplayName("PUT /api/solicitudes/{id}/aprobar con rol USER → 403 Forbidden")
    void aprobarSolicitud_ConRolUser_DeberiaRetornar403() throws Exception {
        UUID fakeId = UUID.randomUUID();

        mockMvc.perform(put("/api/solicitudes/" + fakeId + "/aprobar")
                        .param("observacion", "Aprobado por admin"))
                .andExpect(status().isForbidden());
    }
}
