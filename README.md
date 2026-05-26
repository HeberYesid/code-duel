# Code Duel - Plataforma de Duelos de Programación

¡Bienvenido a **Code Duel**! Una plataforma diseñada para aprender y competir resolviendo retos de programación en tiempo real. Este proyecto no usa frameworks mágicos en el frontend, sino que se basa en **FUNDAMENTOS SÓLIDOS**: Vanilla JS, CSS puro y un backend modular en Spring Boot con una capa de ejecución aislada en Docker.

## 📚 Documentación

Hemos preparado una documentación exhaustiva para que entiendas el **PORQUÉ** detrás de cada decisión, no solo el cómo. 
Si vas a tocar este código, primero lee y entiende los conceptos. La arquitectura es sagrada.

- [Arquitectura General](docs/arquitectura.md): Visión global del sistema, monolito modular y decisiones de diseño.
- [Backend (Spring Boot)](docs/backend.md): Detalles de la API REST, seguridad JWT, manejo de errores y estructura de paquetes.
- [Frontend (Vanilla JS)](docs/frontend.md): Organización por módulos, inyección de dependencias manual y Monaco Editor.
- [Motor de Ejecución (Docker)](docs/docker_code_execution.md): Cómo ejecutamos código de usuarios de forma segura usando contenedores efímeros.

## 🚀 Cómo Empezar

1. **Base de Datos:**
   Asegúrate de tener Docker instalado y levanta la base de datos PostgreSQL:
   ```bash
   docker-compose up -d postgres
   ```

2. **Backend:**
   Navega a la carpeta `backend` y ejecuta el proyecto Spring Boot (las migraciones de Flyway crearán las tablas automáticamente):
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Motor de Ejecución (Importante):**
   Descarga la imagen de Python que usa nuestro motor:
   ```bash
   docker pull python:3.12-alpine
   ```

4. **Frontend:**
   Usa un servidor estático ligero en la carpeta `frontend`:
   ```bash
   cd frontend
   npx serve . 
   # O usa la extensión Live Server en VS Code
   ```

## 🛠️ Stack Tecnológico

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway.
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla ES6+), Monaco Editor.
- **Infraestructura:** Docker (PostgreSQL, Motor de ejecución aislado Python).

---
*Construido con pasión, enfoque en los fundamentos y cero magia negra.*

## 📝 Módulos del Examen Final (Previo-Final-Código)

Se han implementado los siguientes módulos para la entrega del examen final:

### 1. Sistema de Mensajes Internos (Módulo 1)
- Permite la mensajería privada entre usuarios de la plataforma almacenando la información de emisor, receptor, asunto, contenido, estado de lectura y fecha automática de envío en la base de datos.
- **Endpoints**:
  - `POST /api/mensajes`: Envía un mensaje a un destinatario. (Retorna 201)
  - `GET /api/mensajes/bandeja-entrada`: Consulta recibidos para el usuario autenticado. (Retorna 200)
  - `GET /api/mensajes/enviados`: Consulta enviados por el usuario autenticado. (Retorna 200)
  - `PUT /api/mensajes/{id}/leer`: Marca un mensaje recibido como leído. (Retorna 200 o 404)
  - `GET /api/mensajes/no-leidos/count`: Obtiene el conteo de no leídos. (Retorna 200 con `{"count": N}`)

### 2. Flujo de Estados para Solicitudes (Módulo 2)
- Gestión de peticiones de tipo `SOPORTE`, `ACCESO` o `INFORMACIÓN` asociadas a un solicitante, con ciclo de vida `PENDIENTE`, `APROBADA` o `RECHAZADA`.
- **Endpoints**:
  - `POST /api/solicitudes`: Radica una solicitud con estado inicial `PENDIENTE`. (Retorna 201)
  - `GET /api/solicitudes/mis-solicitudes`: Consulta solicitudes del usuario actual. (Retorna 200)
  - `GET /api/solicitudes`: Consulta global de solicitudes del sistema. (Solo administradores `ROLE_ADMIN`, Retorna 200)
  - `PUT /api/solicitudes/{id}/aprobar`: Aprueba solicitud con observación. (Solo `ROLE_ADMIN`, Retorna 200)
  - `PUT /api/solicitudes/{id}/rechazar`: Rechaza solicitud con observación. (Solo `ROLE_ADMIN`, Retorna 200)

### 3. Panel de Administración Thymeleaf (Módulo 3)
- Renderizado del lado del servidor en la ruta `GET /admin/solicitudes/panel`.
- Protegido por roles: sólo accesible por usuarios con rol `ADMIN` (retorna HTTP 403 o redirige si el usuario no tiene permisos).
- Incluye KPI cards con el conteo en tiempo real (Total, Pendientes, Aprobadas, Rechazadas) y una tabla interactiva con diseño de badges diferenciados por estado.

### 4. Pruebas Automatizadas (Módulos 4 y 5)
- **Pruebas unitarias de controlador**: Clase `MensajeControllerTest` utilizando `@WebMvcTest` y mockeando la capa de servicio con `@MockBean`.
- **Pruebas de seguridad e integración**: Clase `SolicitudSecurityTest` levantando todo el contexto con `@SpringBootTest` y simulando roles a nivel de método con `@WithMockUser`.
- Ejecución local con control de hilos para entornos Windows:
  ```bash
  mvn test -DforkCount=0
  ```

