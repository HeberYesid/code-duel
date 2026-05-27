# Módulo 3 – Interfaz Visual: Panel de Solicitudes

## Contexto

El proyecto es exclusivamente una API REST + WebSockets y no posee Thymeleaf. Se agregará la dependencia al `pom.xml`.
Para la autenticación en páginas HTML de administración renderizadas por servidor (Thymeleaf) en un backend con JWT stateless, los navegadores no envían automáticamente el encabezado `Authorization: Bearer <token>` al navegar a una URL. 

Para resolver esto de forma elegante y compatible con flujos REST y MVC, implementaremos:
1. Soporte en `JwtAuthenticationFilter` para extraer el token JWT no solo del encabezado `Authorization` sino también opcionalmente de una cookie llamada `JWT_TOKEN` o del parámetro de consulta `token`.
2. Un controlador de MVC (`@Controller`) que sirva la vista de Thymeleaf y cargue los datos en el modelo.
3. Protección en `SecurityConfig` para `/admin/**` restringido únicamente al rol `ADMIN`.

---

## Archivos Nuevos del Módulo 3

### Estructura de archivos a crear/modificar

```
backend/
├── pom.xml                                      [MODIFY]
├── src/main/java/com/codeduel/backend/
│   ├── controller/
│   │   └── AdminSolicitudController.java        [NEW]
│   └── security/
│       └── JwtAuthenticationFilter.java          [MODIFY] (para leer de cookie/query param)
└── src/main/resources/
    └── templates/
        └── panel.html                           [NEW]
```

---

### [MODIFY] [pom.xml](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/pom.xml)

Agregar la dependencia de Thymeleaf en la sección `<dependencies>`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

---

### [MODIFY] [JwtAuthenticationFilter.java](file:///c:/Users/LabFinancieropc19/Desktop/code-duel/backend/src/main/java/com/codeduel/backend/security/JwtAuthenticationFilter.java)

Modificar la extracción del token para que acepte Cookies o Parámetro de Consulta si el header no está presente:
```java
// Dentro de doFilterInternal:
String jwt = null;
final String authHeader = request.getHeader("Authorization");

if (authHeader != null && authHeader.startsWith("Bearer ")) {
    jwt = authHeader.substring(7);
} else {
    // Intentar leer de cookies
    if (request.getCookies() != null) {
        for (var cookie : request.getCookies()) {
            if ("JWT_TOKEN".equals(cookie.getName())) {
                jwt = cookie.getValue();
                break;
            }
        }
    }
    // Si sigue nulo, intentar con query param (útil para pruebas rápidas en navegador)
    if (jwt == null) {
        jwt = request.getParameter("token");
    }
}

if (jwt == null) {
    filterChain.doFilter(request, response);
    return;
}
```

---

### [NEW] Controlador MVC — `AdminSolicitudController.java`
**Ruta**: `src/main/java/com/codeduel/backend/controller/AdminSolicitudController.java`

```java
package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.model.enums.EstadoSolicitud;
import com.codeduel.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/solicitudes")
@RequiredArgsConstructor
public class AdminSolicitudController {

    private final SolicitudService solicitudService;

    @GetMapping("/panel")
    public String getPanel(Model model) {
        // 1. Panel de indicadores (totales en tiempo real)
        long total = solicitudService.contarTotal();
        long pendientes = solicitudService.contarPorEstado(EstadoSolicitud.PENDIENTE);
        long aprobadas = solicitudService.contarPorEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudService.contarPorEstado(EstadoSolicitud.RECHAZADA);

        model.addAttribute("total", total);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);

        // 2. Tabla de solicitudes
        List<SolicitudResponse> solicitudes = solicitudService.getTodasSolicitudes();
        model.addAttribute("solicitudes", solicitudes);

        return "panel";
    }
}
```

---

### [NEW] Plantilla Thymeleaf — `panel.html`
**Ruta**: `src/main/resources/templates/panel.html`

Utiliza un diseño moderno con Tailwind CSS (vía CDN para renderizar directamente sin compilar) y una paleta de colores premium (oscuro, con acentos vibrantes e indicadores con bordes y tipografía elegante).

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="es">
<head>
    <meta charset="UTF-8">
    <title>Panel de Solicitudes - Admin</title>
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- Tailwind CSS para estilizado premium -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Plus Jakarta Sans', 'sans-serif'],
                    }
                }
            }
        }
    </script>
    <style>
        body {
            background-color: #0B0F19;
            color: #F3F4F6;
        }
    </style>
</head>
<body class="min-h-screen font-sans flex flex-col">

    <!-- Header -->
    <header class="border-b border-gray-800 bg-[#111827]/80 backdrop-blur-md sticky top-0 z-50">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center font-bold text-white shadow-lg shadow-indigo-500/30">CD</div>
                <h1 id="panel-title" class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-400 to-purple-400">Code Duel Admin</h1>
            </div>
            <div class="flex items-center space-x-4">
                <span class="text-sm text-gray-400">Rol: <strong class="text-indigo-400">ADMINISTRADOR</strong></span>
            </div>
        </div>
    </header>

    <!-- Contenido Principal -->
    <main class="flex-grow max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        <!-- Título de Sección -->
        <div class="mb-8">
            <h2 class="text-3xl font-extrabold tracking-tight">Panel General de Solicitudes</h2>
            <p class="text-gray-400 mt-1">Gestión, auditoría y control de peticiones de usuarios.</p>
        </div>

        <!-- Indicadores -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <!-- Total -->
            <div id="stat-total" class="bg-[#1F2937]/50 border border-gray-800 rounded-2xl p-6 flex flex-col justify-between shadow-xl transition hover:border-gray-700">
                <span class="text-sm font-medium text-gray-400">Total Solicitudes</span>
                <span class="text-4xl font-extrabold text-white mt-2" th:text="${total}">0</span>
            </div>
            <!-- Pendientes -->
            <div id="stat-pendientes" class="bg-[#1F2937]/50 border border-amber-500/20 rounded-2xl p-6 flex flex-col justify-between shadow-xl transition hover:border-amber-500/30">
                <span class="text-sm font-medium text-amber-400">Pendientes</span>
                <span class="text-4xl font-extrabold text-amber-400 mt-2" th:text="${pendientes}">0</span>
            </div>
            <!-- Aprobadas -->
            <div id="stat-aprobadas" class="bg-[#1F2937]/50 border border-emerald-500/20 rounded-2xl p-6 flex flex-col justify-between shadow-xl transition hover:border-emerald-500/30">
                <span class="text-sm font-medium text-emerald-400">Aprobadas</span>
                <span class="text-4xl font-extrabold text-emerald-400 mt-2" th:text="${aprobadas}">0</span>
            </div>
            <!-- Rechazadas -->
            <div id="stat-rechazadas" class="bg-[#1F2937]/50 border border-rose-500/20 rounded-2xl p-6 flex flex-col justify-between shadow-xl transition hover:border-rose-500/30">
                <span class="text-sm font-medium text-rose-400">Rechazadas</span>
                <span class="text-4xl font-extrabold text-rose-400 mt-2" th:text="${rechazadas}">0</span>
            </div>
        </div>

        <!-- Tabla -->
        <div class="bg-[#111827] border border-gray-800 rounded-2xl overflow-hidden shadow-2xl">
            <div class="px-6 py-4 border-b border-gray-800 flex justify-between items-center bg-[#1F2937]/20">
                <h3 class="font-semibold text-lg text-gray-200">Listado de Peticiones</h3>
            </div>
            <div class="overflow-x-auto">
                <table id="solicitudes-table" class="min-w-full divide-y divide-gray-800 text-sm">
                    <thead class="bg-[#1F2937]/40 text-gray-400 uppercase text-xs tracking-wider">
                        <tr>
                            <th class="px-6 py-3 text-left font-semibold">Solicitante</th>
                            <th class="px-6 py-3 text-left font-semibold">Tipo</th>
                            <th class="px-6 py-3 text-left font-semibold">Estado</th>
                            <th class="px-6 py-3 text-left font-semibold">Fecha Creación</th>
                            <th class="px-6 py-3 text-left font-semibold">Observación / Respuesta</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-800/60">
                        <tr th:each="sol : ${solicitudes}" class="hover:bg-[#1F2937]/20 transition">
                            <td class="px-6 py-4 font-medium text-white" th:text="${sol.solicitanteUsername}">usuario</td>
                            <td class="px-6 py-4 text-gray-300" th:text="${sol.tipo}">SOPORTE</td>
                            <td class="px-6 py-4">
                                <span th:if="${sol.estado.name() == 'PENDIENTE'}" class="px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">PENDIENTE</span>
                                <span th:if="${sol.estado.name() == 'APROBADA'}" class="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">APROBADA</span>
                                <span th:if="${sol.estado.name() == 'RECHAZADA'}" class="px-3 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">RECHAZADA</span>
                            </td>
                            <td class="px-6 py-4 text-gray-400" th:text="${#temporals.format(sol.fechaCreacion, 'dd/MM/yyyy HH:mm')}">01/01/2026</td>
                            <td class="px-6 py-4 text-gray-400 italic">
                                <span th:text="${sol.observacion != null ? sol.observacion : 'Sin observación registrada'}">Ninguna</span>
                            </td>
                        </tr>
                        <tr th:if="${#lists.isEmpty(solicitudes)}">
                            <td colspan="5" class="px-6 py-8 text-center text-gray-500">No hay solicitudes registradas en el sistema.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </main>

    <!-- Footer -->
    <footer class="border-t border-gray-800 py-6 bg-[#0B0F19]">
        <div class="max-w-7xl mx-auto px-4 text-center text-sm text-gray-500">
            &copy; 2026 Code Duel Platform. Todos los derechos reservados.
        </div>
    </footer>
</body>
</html>
```

---

## Verificación

### Acceso Exitoso (ADMIN)
- Iniciar sesión con un usuario con rol `ADMIN`.
- Guardar el token en una cookie llamada `JWT_TOKEN` desde la consola de desarrollador del navegador (o pasarlo en la URL: `http://localhost:8080/admin/solicitudes/panel?token=...`).
- Cargar la página y verificar los indicadores y la tabla.

### Acceso Rechazado (USER / Anónimo)
- Intentar acceder sin token (anónimo): debe retornar 403 Forbidden (o 401 si no está autenticado).
- Intentar acceder con un token de un usuario regular (rol `USER`): debe retornar 403 Forbidden.
