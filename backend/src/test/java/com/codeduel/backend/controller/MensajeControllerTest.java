package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.security.JwtAuthenticationFilter;
import com.codeduel.backend.security.JwtService;
import com.codeduel.backend.service.MensajeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MensajeController.class)
@Import(JwtAuthenticationFilter.class)
@DisplayName("MensajeController Unit Tests")
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MensajeService mensajeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/mensajes/bandeja-entrada con usuario autenticado → 200 OK")
    void getBandejaEntrada_ConUsuarioAutenticado_DeberiaRetornar200() throws Exception {
        MensajeResponse mensaje = MensajeResponse.builder()
                .id(UUID.randomUUID())
                .emisor("emisor")
                .receptor("testuser")
                .asunto("Asunto")
                .contenido("Contenido")
                .leido(false)
                .fechaEnvio(LocalDateTime.now())
                .build();

        when(mensajeService.getBandejaEntrada("testuser")).thenReturn(List.of(mensaje));

        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/mensajes/bandeja-entrada sin autenticación → 401 o 403")
    void getBandejaEntrada_SinAutenticacion_DeberiaRetornar4xx() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/mensajes con cuerpo vacío → 400 Bad Request")
    void enviarMensaje_ConCuerpoVacio_DeberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
