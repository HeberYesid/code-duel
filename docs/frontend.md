# Documentación del Frontend (Vanilla JS)

¡Escúchame bien! Antes de meter React, Vue o Angular a lo loco, hay que entender cómo funciona el DOM y el BOM. Por eso nuestro frontend es Vanilla JavaScript puro. ¡Así se forja el carácter!

## Estructura del Proyecto

```
frontend/
├── index.html        # Único punto de entrada (SPA)
├── css/
│   └── styles.css    # CSS Puro, variables CSS para temas (Dark/Light)
└── js/
    ├── app.js        # Entry point de JS, ruteo básico
    ├── auth.js       # Lógica de Login/Registro y manejo del JWT
    ├── api.js        # Wrapper alrededor de fetch() para incluir el JWT
    ├── challenges.js # Renderizado de la lista de retos
    ├── practice.js   # Lógica del motor de ejecución y DOM de la vista
    └── editor.js     # Inicialización y abstracción de Monaco Editor
```

## Ruteo Básico (SPA Manual)

No estamos recargando la página. Ocultamos y mostramos secciones usando CSS (`display: none`).
En `index.html` tenemos contenedores como `#auth-view`, `#dashboard-view`, `#practice-view`.
Cuando el usuario interactúa, llamamos a funciones de transición (ej. `showView('dashboard-view')`) que cambian la clase `.hidden` de estos divs.

## Interfaz de Red (`api.js`)

En lugar de hacer `fetch()` desperdigados por todo el código, centralizamos las peticiones en un módulo.
Esto nos permite:
1. Inyectar automáticamente el header `Authorization: Bearer <token>` de localStorage.
2. Manejar globalmente los errores 401 (Si el token expiró, redirigimos al login limpiando el storage).
3. Estandarizar el parseo del JSON.

## Integración con Monaco Editor

Monaco es el core de VS Code. Es pesado, así que lo cargamos asíncronamente desde un CDN usando AMD (Asynchronous Module Definition), no módulos ES normales.

```javascript
// js/editor.js
require.config({ paths: { 'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs' }});
require(['vs/editor/editor.main'], function() {
    window.monacoEditor = monaco.editor.create(document.getElementById('editor-container'), {
        value: "def solve():\n    pass",
        language: 'python',
        theme: 'vs-dark'
    });
});
```
Abstraemos esto en `editor.js` para que la vista `practice.js` solo llame a `getEditorValue()` sin preocuparse de cómo funciona Monaco por debajo.
