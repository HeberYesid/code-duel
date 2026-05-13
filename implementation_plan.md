# Plan de Desarrollo: Plataforma de Duelos de Programación

Este documento detalla el plan arquitectónico y de desarrollo módulo por módulo para la plataforma. El enfoque es incremental, asegurando que cada fase entregue valor funcional y se pueda probar de forma independiente antes de pasar a la siguiente complejidad.

## ✅ Decisiones de Arquitectura Confirmadas

- **Base de Datos:** PostgreSQL con **Flyway** para las migraciones.
- **Docker (Seguridad):** Contenedores aislados sin red, límite de memoria 128MB, timeout de 3-5 segundos, y de solo lectura.
- **Frontend:** HTML, CSS y JS Vanilla estructurado mediante módulos simples y básicos por vista.
- **Matchmaking:** Opción manual para cancelar, con un timeout automático por seguridad si el WebSocket se desconecta inesperadamente.
- **Testing:** Se prioriza el desarrollo de funcionalidades (testing manual). Las pruebas automatizadas (unitarias/integración) se dejarán para una fase posterior.

---

## Fases de Desarrollo Propuestas

### Fase 1: Cimientos y Gestión de Usuarios (Auth & Users)
**Objetivo:** Establecer la base del Monolito Modular en Spring Boot, conexión a PostgreSQL, seguridad JWT y estructura básica del frontend.

*   **Backend:**
    *   Setup del proyecto Spring Boot (Web, Data JPA, Security, PostgreSQL driver).
    *   Implementación de entidades `User` y `Profile`.
    *   Endpoints de Registro y Login (Filtros y proveedores de Autenticación JWT).
    *   Gestión de excepciones global (`@ControllerAdvice`).
*   **Frontend:**
    *   Setup de estructura (index.html, styles.css, scripts modulares).
    *   Vistas de Login y Registro.
    *   Almacenamiento del JWT (`localStorage`) e interceptor básico en JS (usando `fetch` API) para peticiones autenticadas.

---

### Fase 2: El Núcleo de Problemas (Challenges & Test Cases)
**Objetivo:** Permitir la creación, almacenamiento y consulta de los retos de programación.

*   **Backend:**
    *   Entidades `Challenge` y `TestCase`.
    *   Servicio y repositorios para consultar retos.
    *   Endpoints CRUD internos (útiles para poblar la base de datos) y lógica para obtener un reto aleatorio por `DifficultyLevel`.
*   **Frontend:**
    *   Vista de exploración/listado de retos.

---

### Fase 3: Motor de Ejecución de Código (Code Execution Core)
**Objetivo:** Componente crítico. Ejecutar código Python de forma segura mediante Docker aislando resultados.

*   **Backend:**
    *   Integración con Docker (usando *Docker Java API Client* o comandos de consola encapsulados).
    *   Implementación de `CodeExecutionService`.
    *   Lógica para: Inyectar `TestCase` (stdin), ejecutar script, capturar `output` (stdout/stderr) y comparar con `expectedOutput`.
    *   Manejo de estados de `SubmissionStatus` (ACCEPTED, WRONG_ANSWER, TIME_LIMIT, ERROR).
*   **Frontend (Modo Práctica):**
    *   Integrar un editor de código ligero en el navegador (ej. CodeMirror básico).
    *   Pantalla aislada donde un usuario puede escribir código para un reto, enviarlo por REST, y ver si su código compila y pasa las pruebas.

---

### Fase 4: Infraestructura en Tiempo Real y Matchmaking
**Objetivo:** Conectar usuarios vía WebSockets y crear el sistema de emparejamiento.

*   **Backend:**
    *   Configuración de Spring WebSocket con STOMP.
    *   Autenticación en WebSockets (validar JWT en el handshake/connect).
    *   Implementación de `MatchmakingService` y colas en memoria (`WaitingRoom`) separadas por dificultad.
*   **Frontend:**
    *   Conexión WebSocket persistente al iniciar sesión exitosamente.
    *   UI con botones de "Buscar Partida" por dificultad (Easy, Medium, Hard).
    *   Vista de "Buscando oponente..." con botón para "Cancelar Búsqueda".

---

### Fase 5: La Arena de Duelos (Duels & Live Submissions)
**Objetivo:** Orquestar la partida entre dos usuarios conectados.

*   **Backend:**
    *   Implementación de `DuelService`.
    *   Flujo: Al encontrar match -> crear `Duel` en DB -> seleccionar `Challenge` -> notificar a los 2 jugadores por STOMP (`MATCH_FOUND`).
    *   Recepción de intentos (`CodeSubmission`) durante el duelo.
    *   Evaluación de código asíncrona delegada a la Fase 3, y publicación inmediata de resultados a la sala del duelo por WebSocket (`SUBMISSION_RESULT`).
    *   Detección del primer jugador con todos los tests en `ACCEPTED` -> Cerrar duelo, declarar ganador (`DUEL_FINISHED`).
*   **Frontend:**
    *   Vista de la Arena (Duelo activo).
    *   División de pantalla: Editor de código + Descripción del problema.
    *   Panel de feedback en tiempo real: Progreso visual propio y del oponente.
    *   Modal de Fin de Partida anunciando victoria/derrota/empate.

---

### Fase 6: Progresión, Clasificación y Notificaciones
**Objetivo:** Sistema competitivo, cálculo de ELO/puntos y experiencia social.

*   **Backend:**
    *   Implementación de `RankingService` y `NotificationService`.
    *   Entidades `ScoreEntry`, `LeaderboardContext` y `Notification`.
    *   Lógica para recalcular el puntaje de ambos usuarios basado en el resultado del duelo.
    *   Generación y almacenamiento de notificaciones (`RANK_CHANGE`).
*   **Frontend:**
    *   Vista de Leaderboard global.
    *   Sistema UI de campana/alertas para notificaciones.
    *   Actualización del Perfil de usuario con estadísticas (Victorias, Derrotas, Puntaje).

---

## Plan de Verificación

### Pruebas Automatizadas (Si se aprueban en las Open Questions)
- Pruebas unitarias de la lógica de matchmaking y cálculo de puntajes.
- Pruebas de integración simulando el ciclo completo de vida del contenedor Docker (Casos de éxito, timeout, syntax error).

### Verificación Manual
- Despliegue de la infraestructura local usando `docker-compose` (PostgreSQL + app Spring).
- Ejecutar dos instancias de navegador (o modo incógnito) con diferentes usuarios.
- Realizar flujos completos: Cola -> Match -> Duelo -> Envío de código -> Victoria -> Reflejo en Leaderboard.
