package com.codeduel.backend.controller;

import com.codeduel.backend.security.JwtService;
import com.codeduel.backend.service.MensajeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MensajeController.class)
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MensajeService mensajeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    // Escenario 1: GET /api/mensajes/bandeja-entrada con usuario autenticado -> HTTP 200 OK
    @Test
    @WithMockUser(username = "user1")
    void obtenerBandejaEntrada_ConUsuarioAutenticado_DeberiaRetornar200() throws Exception {
        when(mensajeService.obtenerBandejaEntrada("user1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().isOk());
    }

    // Escenario 2: GET /api/mensajes/bandeja-entrada sin ninguna autenticación -> HTTP 401 o 403
    @Test
    void obtenerBandejaEntrada_SinAutenticacion_DeberiaRetornar401o403() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().is4xxClientError()); // matches 401 or 403
    }

    // Escenario 3: POST /api/mensajes con cuerpo vacío o campos obligatorios faltantes -> HTTP 400 Bad Request
    @Test
    @WithMockUser(username = "user1")
    void enviarMensaje_ConCamposFaltantes_DeberiaRetornar400() throws Exception {
        // Enviar cuerpo vacío
        mockMvc.perform(post("/api/mensajes")
                        .with(csrf()) // include CSRF token if active in tests
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Enviar cuerpo con destinatario faltante
        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Hola\",\"contenido\":\"Test\"}"))
                .andExpect(status().isBadRequest());
    }
}
