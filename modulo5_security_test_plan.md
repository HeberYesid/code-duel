# Módulo 5 – Pruebas de Seguridad con @WithMockUser

## Contexto

Este módulo define una clase de pruebas de integración (`SolicitudSecurityTest`) para validar los niveles de autorización y acceso a los endpoints del Módulo 2 bajo diferentes roles y escenarios de autenticación.

A diferencia del Módulo 4, este módulo requiere levantar el contexto completo de Spring (`@SpringBootTest`) y conectarse a la base de datos de pruebas (H2 en memoria).

---

## Decisiones Técnicas Críticas

> [!IMPORTANT]
> **Creación de usuario en base de datos para la prueba 2 (201 Created)**:
> El endpoint `POST /api/solicitudes` realiza una consulta a la base de datos para recuperar al usuario autenticado actual y asociarlo como el `solicitante` de la solicitud. Si simulamos un usuario con `@WithMockUser(username = "normaluser")` pero dicho usuario no existe en la base de datos de pruebas (H2), la capa de servicios lanzará un error `404 Not Found` en lugar de crear la solicitud.
> 
> Por lo tanto, en la prueba 2 debemos registrar previamente al usuario en el `UserRepository` antes de ejecutar la petición HTTP.

> [!NOTE]
> **Prueba de acceso denegado (403 Forbidden) en aprobación**:
> El endpoint `PUT /api/solicitudes/{id}/aprobar` estará protegido a nivel de configuración en `SecurityConfig` para que solo usuarios con el rol `ADMIN` (es decir, authority `ROLE_ADMIN`) tengan acceso.
> Cuando simulamos un usuario con rol `USER` usando `@WithMockUser(roles = "USER")` y ejecutamos la petición, la petición será interceptada y rechazada por la cadena de filtros de Spring Security antes de alcanzar el controlador o el servicio. Así se garantiza el retorno del código HTTP 403 sin importar si el `{id}` existe o no en la base de datos.

---

## Archivos Nuevos del Módulo 5

### [NEW] Clase de Pruebas — `SolicitudSecurityTest.java`
**Ruta**: `src/test/java/com/codeduel/backend/controller/SolicitudSecurityTest.java`

```java
package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudRequest;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.TipoSolicitud;
import com.codeduel.backend.repository.SolicitudRepository;
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

    @BeforeEach
    void setup() {
        solicitudRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== ESCENARIO 1 ====================
    @Test
    @DisplayName("POST /api/solicitudes sin autenticación — HTTP 401 o 403")
    void createSolicitud_Anonymous_ShouldReturnUnauthorizedOrForbidden() throws Exception {
        SolicitudRequest request = new SolicitudRequest(TipoSolicitud.SOPORTE, "Ayuda técnica");

        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // U isUnauthorized() según configuración de entrypoint
    }

    // ==================== ESCENARIO 2 ====================
    @Test
    @WithMockUser(username = "normaluser", roles = "USER")
    @DisplayName("POST /api/solicitudes con usuario autenticado (USER) — HTTP 201 Created")
    void createSolicitud_Authenticated_ShouldReturn201() throws Exception {
        // Registrar previamente al usuario en la BD de pruebas para evitar error 404
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

    // ==================== ESCENARIO 3 ====================
    @Test
    @WithMockUser(username = "regularuser", roles = "USER")
    @DisplayName("PUT /api/solicitudes/{id}/aprobar con rol USER — HTTP 403 Forbidden")
    void approveSolicitud_AsUser_ShouldReturn403() throws Exception {
        // Se envía a una solicitud inexistente (ID 9999) con parámetro de observación
        mockMvc.perform(put("/api/solicitudes/9999/aprobar")
                        .with(csrf())
                        .param("observacion", "Aprobación inválida")
                        .contentType(MediaType.APPLICATION_JSON))
                // Debe ser denegado por el filtro de seguridad antes de buscar el registro
                .andExpect(status().isForbidden());
    }
}
```

---

## Verificación

### Comando para ejecución
Las pruebas de integración de seguridad deben ejecutarse y pasar con:
```bash
./mvnw test -Dtest=SolicitudSecurityTest
```
O de forma alternativa:
```bash
mvn test -Dtest=SolicitudSecurityTest
```

### Comprobaciones técnicas clave
1. **Limpieza de Base de Datos**: `@BeforeEach` garantiza un estado consistente eliminando registros de solicitudes y usuarios antes de cada prueba.
2. **Rol en `@WithMockUser`**: Spring Security asocia automáticamente el prefijo `ROLE_` a los valores de `roles` pasados en la anotación (ej: `roles = "USER"` equivale a tener el authority `ROLE_USER`). Esto casa perfectamente con las reglas de `hasRole("ADMIN")` que mapearemos en `SecurityConfig` para la ruta `/api/solicitudes/**/aprobar`.
