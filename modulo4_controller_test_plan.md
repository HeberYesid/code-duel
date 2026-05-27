# Módulo 4 – Pruebas de Controlador con @WebMvcTest

## Contexto

Este módulo define una clase de pruebas unitarias (`MensajeControllerTest`) para validar el comportamiento del controlador de mensajería desarrollado en el Módulo 1.

Al utilizar `@WebMvcTest`, Spring Boot no levantará el contexto completo de la base de datos, sino únicamente los beans de la capa de presentación (controladores, filtros de seguridad, etc.). Para que las pruebas de seguridad funcionen adecuadamente bajo este esquema, debemos mockear las dependencias de la infraestructura de seguridad:
- `JwtService`
- `CustomUserDetailsService` / `UserDetailsService`
- `MensajeService` (capa de negocio del controlador)

---

## Archivos Nuevos del Módulo 4

### [NEW] Clase de Pruebas — `MensajeControllerTest.java`
**Ruta**: `src/test/java/com/codeduel/backend/controller/MensajeControllerTest.java`

```java
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // Se mockean los beans requeridos por SecurityConfig y JwtAuthenticationFilter
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    // ==================== ESCENARIO 1 ====================
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

    // ==================== ESCENARIO 2 ====================
    @Test
    @DisplayName("GET /api/mensajes/bandeja-entrada sin autenticación — HTTP 401 o 403")
    void getBandejaEntrada_Anonymous_ShouldReturnUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada")
                        .contentType(MediaType.APPLICATION_JSON))
                // Retorna 403 porque Spring Security por defecto arroja Forbidden/Unauthorized ante fallos de filtro
                .andExpect(status().isForbidden()); 
    }

    // ==================== ESCENARIO 3 ====================
    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/mensajes con campos faltantes o vacíos — HTTP 400 Bad Request")
    void createMensaje_InvalidBody_ShouldReturn400() throws Exception {
        // Objeto vacío con campos nulos/blancos que disparan @NotBlank
        MensajeRequest invalidRequest = new MensajeRequest("", "", "");

        mockMvc.perform(post("/api/mensajes")
                        .with(csrf()) // Se incluye CSRF token para evitar que falle por seguridad antes de validación
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.messages").isArray());
    }
}
```

---

## Verificación

### Comando para ejecución
Las pruebas unitarias especificadas deben ejecutarse y pasar con:
```bash
./mvnw test -Dtest=MensajeControllerTest
```
O de forma alternativa si no está el wrapper en local:
```bash
mvn test -Dtest=MensajeControllerTest
```

### Elementos clave a comprobar
1. **Mockeo de la Capa de Seguridad**: Sin `@MockBean private JwtService jwtService` y `UserDetailsService`, el contexto de Spring fallará al iniciarse porque no puede resolver esas dependencias para inyectarlas en el filtro de seguridad.
2. **Uso de `.with(csrf())`**: Aunque el CSRF esté deshabilitado para APIs REST (`csrf().disable()`), incluirlo en pruebas web de integración por seguridad es una buena práctica y previene comportamientos inesperados de los filtros de MockMvc.
3. **Mapeo de Errores de Validación**: Comprobar que el `GlobalExceptionHandler` de la aplicación intercepte la excepción `MethodArgumentNotValidException` y retorne la estructura JSON esperada con código HTTP 400.
