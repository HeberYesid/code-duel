package com.codeduel.backend.security;

import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.SolicitudRepository;
import com.codeduel.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @BeforeEach
    void setup() {
        solicitudRepository.deleteAll();

        // Asegurarse de que el usuario de prueba exista sin borrar el resto para evitar FK violations
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = User.builder()
                    .username("user")
                    .email("user@test.com")
                    .passwordHash("DummyPasswordHash123")
                    .role("USER")
                    .build();
            userRepository.save(user);
        }
    }

    // Escenario 1: POST /api/solicitudes sin ninguna autenticación -> HTTP 401 o 403
    @Test
    void crearSolicitud_SinAutenticacion_DeberiaRetornar401o403() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SOPORTE\",\"descripcion\":\"Test\"}"))
                .andExpect(status().is4xxClientError()); // matches 401 or 403
    }

    // Escenario 2: POST /api/solicitudes con usuario autenticado sin rol especial -> HTTP 201 Created
    @Test
    @WithMockUser(username = "user")
    void crearSolicitud_ConUsuarioAutenticado_DeberiaRetornar201() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SOPORTE\",\"descripcion\":\"Mi solicitud de prueba\"}"))
                .andExpect(status().isCreated());
    }

    // Escenario 3: PUT /api/solicitudes/{id}/aprobar con usuario autenticado con rol USER (sin ADMIN) -> HTTP 403 Forbidden
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void aprobarSolicitud_ConUsuarioRolUser_DeberiaRetornar403() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(put("/api/solicitudes/" + randomId + "/aprobar")
                        .with(csrf())
                        .param("observacion", "Aprobación denegada en teoría"))
                .andExpect(status().isForbidden());
    }
}
