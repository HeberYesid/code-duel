package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.security.JwtService;
import com.codeduel.backend.service.MensajeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MensajeController.class)
@DisplayName("MensajeController Unit Tests")
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MensajeService mensajeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/mensajes/bandeja-entrada con usuario autenticado — HTTP 200 OK")
    void getBandejaEntrada_Authenticated_ShouldReturn200() throws Exception {
        MensajeResponse msg = MensajeResponse.builder()
                .id(1L)
                .emisorUsername("otheruser")
                .receptorUsername("testuser")
                .asunto("Hola")
                .contenido("Mensaje de prueba")
                .leido(false)
                .fechaEnvio(LocalDateTime.now())
                .build();

        when(mensajeService.getBandejaEntrada("testuser")).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/mensajes/bandeja-entrada")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].emisorUsername").value("otheruser"))
                .andExpect(jsonPath("$[0].receptorUsername").value("testuser"))
                .andExpect(jsonPath("$[0].asunto").value("Hola"));
    }

    @Test
    @DisplayName("GET /api/mensajes/bandeja-entrada sin autenticación — HTTP 401 o 403")
    void getBandejaEntrada_Anonymous_ShouldReturnUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/mensajes con campos faltantes o vacíos — HTTP 400 Bad Request")
    void createMensaje_InvalidBody_ShouldReturn400() throws Exception {
        MensajeRequest invalidRequest = new MensajeRequest("", "", "");

        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.messages").isArray());
    }
}
