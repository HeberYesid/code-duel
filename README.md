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
