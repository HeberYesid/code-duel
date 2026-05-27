# Módulo 1 – Sistema de Mensajes Internos

## Contexto del Proyecto

El proyecto **Code Duel** es un backend Spring Boot 3.2.5 con:
- **Paquete base**: `com.codeduel.backend`
- **Seguridad**: JWT stateless con `JwtAuthenticationFilter` + `CustomUserDetailsService`
- **BD**: PostgreSQL en producción, H2 en tests
- **Migraciones**: Flyway (5 migraciones existentes: V1–V5)
- **ORM**: JPA/Hibernate con `ddl-auto: validate`
- **Rama actual**: `master` → se creará rama `previo-final-02230131061`

> [!IMPORTANT]
> El modelo [User.java](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/model/User.java) **NO tiene campo `role`**. Actualmente, [CustomUserDetailsService](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/security/CustomUserDetailsService.java) asigna `Collections.emptyList()` como authorities. Esto es relevante para los Módulos 2, 3 y 5 que requieren distinguir entre USER y ADMIN. **Se necesitará agregar un campo `role` al modelo User** como parte de un paso previo compartido por todos los módulos.

---

## Preguntas Abiertas

> [!WARNING]
> Estas decisiones impactan la implementación de TODOS los módulos.

1. **¿Cómo manejar la ausencia del campo `role` en `User`?**
   - **Opción A (Recomendada)**: Agregar un campo `role` (VARCHAR, default `'USER'`) a la tabla `users` con una nueva migración Flyway `V6__add_role_to_users.sql`, actualizar la entidad `User.java` y `CustomUserDetailsService` para asignar el `ROLE_` correspondiente.
   - **Opción B**: Crear una tabla separada `user_roles` (muchos a muchos).
   - La opción A es más simple y suficiente para USER/ADMIN.

2. **¿Debo buscar al destinatario del mensaje por `username` (string) o por `id` (UUID)?**
   - El enunciado dice "indicando el nombre de usuario del destinatario", lo que sugiere buscar por `username`.
   - Propongo que el DTO de creación reciba `destinatarioUsername` (String).

3. **¿Git no está en el PATH del sistema?** — `git` no fue reconocido. ¿Hay un path específico o debo usar otra herramienta para crear la rama?

---

## Paso Previo Compartido — Agregar `role` a `User`

> [!NOTE]
> Este paso se ejecuta una sola vez y beneficia a los Módulos 1 (no estrictamente necesario pero consistente), 2, 3 y 5.

### [NEW] V6__add_role_to_users.sql
**Ruta**: `src/main/resources/db/migration/V6__add_role_to_users.sql`

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

### [MODIFY] [User.java](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/model/User.java)

Agregar:
```java
@Column(nullable = false, length = 20)
@Builder.Default
private String role = "USER";
```

### [MODIFY] [CustomUserDetailsService.java](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/security/CustomUserDetailsService.java)

Cambiar `Collections.emptyList()` por authorities basados en el rol:
```java
return new User(
        user.getUsername(),
        user.getPasswordHash(),
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
);
```

### [MODIFY] [SecurityConfig.java](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/security/SecurityConfig.java)

Agregar reglas de autorización para los nuevos endpoints:
```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/ws/**").permitAll()
// Módulo 1 - Mensajes (cualquier autenticado)
.requestMatchers("/api/mensajes/**").authenticated()
// Módulo 2 - Solicitudes
.requestMatchers(HttpMethod.GET, "/api/solicitudes").hasRole("ADMIN")
.requestMatchers("/api/solicitudes/*/aprobar", "/api/solicitudes/*/rechazar").hasRole("ADMIN")
.requestMatchers("/api/solicitudes/**").authenticated()
// Módulo 3 - Panel admin
.requestMatchers("/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

---

## Archivos Nuevos del Módulo 1

### Estructura de archivos a crear

```
src/main/java/com/codeduel/backend/
├── model/
│   └── Mensaje.java                    [NEW]
├── dto/
│   ├── MensajeRequest.java             [NEW]
│   ├── MensajeResponse.java            [NEW]
│   └── UnreadCountResponse.java        [NEW]
├── repository/
│   └── MensajeRepository.java          [NEW]
├── service/
│   └── MensajeService.java             [NEW]
└── controller/
    └── MensajeController.java          [NEW]

src/main/resources/db/migration/
└── V7__mensajes.sql                    [NEW]
```

---

### [NEW] Migración Flyway — `V7__mensajes.sql`
**Ruta**: `src/main/resources/db/migration/V7__mensajes.sql`

```sql
CREATE TABLE mensajes (
    id          BIGSERIAL PRIMARY KEY,
    emisor_id   UUID NOT NULL REFERENCES users(id),
    receptor_id UUID NOT NULL REFERENCES users(id),
    asunto      VARCHAR(255) NOT NULL,
    contenido   TEXT NOT NULL,
    leido       BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensajes_receptor ON mensajes(receptor_id);
CREATE INDEX idx_mensajes_emisor ON mensajes(emisor_id);
```

---

### [NEW] Entidad — `Mensaje.java`
**Ruta**: `src/main/java/com/codeduel/backend/model/Mensaje.java`

```java
package com.codeduel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id", nullable = false)
    private User emisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id", nullable = false)
    private User receptor;

    @Column(nullable = false, length = 255)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @CreationTimestamp
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;
}
```

---

### [NEW] DTO Request — `MensajeRequest.java`
**Ruta**: `src/main/java/com/codeduel/backend/dto/MensajeRequest.java`

```java
package com.codeduel.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MensajeRequest {

    @NotBlank(message = "El destinatario es obligatorio")
    private String destinatarioUsername;

    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;
}
```

---

### [NEW] DTO Response — `MensajeResponse.java`
**Ruta**: `src/main/java/com/codeduel/backend/dto/MensajeResponse.java`

```java
package com.codeduel.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MensajeResponse {
    private Long id;
    private String emisorUsername;
    private String receptorUsername;
    private String asunto;
    private String contenido;
    private Boolean leido;
    private LocalDateTime fechaEnvio;
}
```

---

### [NEW] DTO Response — `UnreadCountResponse.java`
**Ruta**: `src/main/java/com/codeduel/backend/dto/UnreadCountResponse.java`

```java
package com.codeduel.backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UnreadCountResponse {
    private long count;
}
```

---

### [NEW] Repositorio — `MensajeRepository.java`
**Ruta**: `src/main/java/com/codeduel/backend/repository/MensajeRepository.java`

```java
package com.codeduel.backend.repository;

import com.codeduel.backend.model.Mensaje;
import com.codeduel.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByReceptorOrderByFechaEnvioDesc(User receptor);

    List<Mensaje> findByEmisorOrderByFechaEnvioDesc(User emisor);

    long countByReceptorAndLeidoFalse(User receptor);
}
```

---

### [NEW] Servicio — `MensajeService.java`
**Ruta**: `src/main/java/com/codeduel/backend/service/MensajeService.java`

```java
package com.codeduel.backend.service;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.dto.UnreadCountResponse;
import com.codeduel.backend.exception.BadRequestException;
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

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;

    @Transactional
    public MensajeResponse enviarMensaje(String emisorUsername, MensajeRequest request) {
        User emisor = userRepository.findByUsername(emisorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Emisor no encontrado"));

        User receptor = userRepository.findByUsername(request.getDestinatarioUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destinatario no encontrado: " + request.getDestinatarioUsername()));

        if (emisor.getId().equals(receptor.getId())) {
            throw new BadRequestException("No puedes enviarte mensajes a ti mismo");
        }

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
        User receptor = findUser(username);
        return mensajeRepository.findByReceptorOrderByFechaEnvioDesc(receptor)
                .stream().map(this::toResponse).toList();
    }

    public List<MensajeResponse> getEnviados(String username) {
        User emisor = findUser(username);
        return mensajeRepository.findByEmisorOrderByFechaEnvioDesc(emisor)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MensajeResponse marcarComoLeido(Long mensajeId, String username) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mensaje no encontrado con id: " + mensajeId));

        if (!mensaje.getReceptor().getUsername().equals(username)) {
            throw new AccessDeniedException("No puedes marcar como leído un mensaje que no es tuyo");
        }

        mensaje.setLeido(true);
        mensaje = mensajeRepository.save(mensaje);
        return toResponse(mensaje);
    }

    public UnreadCountResponse contarNoLeidos(String username) {
        User receptor = findUser(username);
        long count = mensajeRepository.countByReceptorAndLeidoFalse(receptor);
        return new UnreadCountResponse(count);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private MensajeResponse toResponse(Mensaje m) {
        return MensajeResponse.builder()
                .id(m.getId())
                .emisorUsername(m.getEmisor().getUsername())
                .receptorUsername(m.getReceptor().getUsername())
                .asunto(m.getAsunto())
                .contenido(m.getContenido())
                .leido(m.getLeido())
                .fechaEnvio(m.getFechaEnvio())
                .build();
    }
}
```

---

### [NEW] Controlador — `MensajeController.java`
**Ruta**: `src/main/java/com/codeduel/backend/controller/MensajeController.java`

```java
package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MensajeRequest;
import com.codeduel.backend.dto.MensajeResponse;
import com.codeduel.backend.dto.UnreadCountResponse;
import com.codeduel.backend.service.MensajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @PostMapping
    public ResponseEntity<MensajeResponse> enviarMensaje(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MensajeRequest request) {
        MensajeResponse response = mensajeService.enviarMensaje(
                userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bandeja-entrada")
    public ResponseEntity<List<MensajeResponse>> getBandejaEntrada(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.getBandejaEntrada(userDetails.getUsername()));
    }

    @GetMapping("/enviados")
    public ResponseEntity<List<MensajeResponse>> getEnviados(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.getEnviados(userDetails.getUsername()));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<MensajeResponse> marcarComoLeido(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.marcarComoLeido(id, userDetails.getUsername()));
    }

    @GetMapping("/no-leidos/count")
    public ResponseEntity<UnreadCountResponse> contarNoLeidos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.contarNoLeidos(userDetails.getUsername()));
    }
}
```

---

## Códigos HTTP Esperados

| Endpoint | Éxito | Error |
|---|---|---|
| `POST /api/mensajes` | 201 Created | 400 (validación), 404 (destinatario no encontrado) |
| `GET /api/mensajes/bandeja-entrada` | 200 OK | 401/403 (no autenticado) |
| `GET /api/mensajes/enviados` | 200 OK | 401/403 (no autenticado) |
| `PUT /api/mensajes/{id}/leer` | 200 OK | 404 (no existe), 403 (no es su mensaje) |
| `GET /api/mensajes/no-leidos/count` | 200 OK | 401/403 (no autenticado) |

---

## Verificación

### Pruebas automáticas
- El Módulo 4 cubre las pruebas unitarias con `@WebMvcTest` para este controlador.
- Se puede ejecutar: `./mvnw test` para verificar que las pruebas existentes no se rompen.

### Verificación manual
- Crear dos usuarios vía `/api/auth/register`.
- Enviar un mensaje de uno a otro vía `POST /api/mensajes`.
- Consultar bandeja de entrada y mensajes enviados.
- Marcar como leído y verificar el count de no leídos.

---

## Orden de Implementación

1. Paso previo: migración V6 (role) + modificar User + CustomUserDetailsService + SecurityConfig
2. Migración V7 (mensajes)
3. Entidad `Mensaje.java`
4. DTOs: `MensajeRequest`, `MensajeResponse`, `UnreadCountResponse`
5. Repositorio `MensajeRepository`
6. Servicio `MensajeService`
7. Controlador `MensajeController`
