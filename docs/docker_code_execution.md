# Motor de Ejecución en Docker

¡Llegamos a la joya de la corona! Ejecutar código de extraños en nuestro servidor es **EXTREMADAMENTE PELIGROSO**. Si hacemos esto mal, alguien nos puede hacer un *Fork Bomb*, leer nuestros archivos `.env`, o minar criptomonedas. 

## El Modelo de Seguridad

Usamos contenedores Docker efímeros configurados bajo un estricto principio de mínimos privilegios:

1. **Aislamiento de Red:** `--network none`. El código del usuario NO puede hacer peticiones HTTP.
2. **Sistema de Archivos Read-Only:** `--read-only`. No pueden modificar los archivos del contenedor ni crear shells inversas persistentes.
3. **Memoria y CPU Limitada:** `--memory 128m --cpus 0.5`. Evitamos que nos saturen el servidor (Out Of Memory).
4. **Límite de Procesos:** `--pids-limit 50`. Previene los famosos *Fork Bombs* (creación recursiva infinita de procesos).
5. **Directorio Temporal (tmpfs):** Montamos `/tmp` en memoria RAM o disco temporal mapeado desde el Host solo para los archivos estrictamente necesarios.

## El Flujo de Ejecución

1. **El Usuario Envía Código:** El frontend manda el código fuente en Python a `/api/submissions/practice`.
2. **Java Prepara el Entorno:** 
   - El `CodeExecutionService` crea un directorio temporal en el Host (`/tmp/codeduel-exec-UUID/`).
   - Guarda allí el código del usuario como `solution.py`.
   - Recupera los Casos de Prueba (TestCases) del reto en la base de datos y los guarda como `test_cases.json`.
   - Copia nuestro script maestro `runner.py` a ese directorio.
3. **Se Levanta el Contenedor:**
   - Java ejecuta mediante `ProcessBuilder` el comando `docker run`.
   - Monta el directorio temporal del Host como volumen de solo lectura (`-v /tmp/UUID:/app:ro`) dentro del contenedor.
4. **Python Evalúa (Dentro de Docker):**
   - El contenedor ejecuta `python3 /app/runner.py`.
   - `runner.py` lee `test_cases.json` y ejecuta iterativamente la solución del usuario contra los inputs usando `subprocess.run()`.
   - Por cada test, mide el tiempo, compara el `stdout` contra el valor esperado, y evalúa timeouts o runtime errors (`stderr`).
   - `runner.py` imprime a la consola (su propio stdout) un gran arreglo JSON con todos los resultados.
5. **Java Recopila Resultados:**
   - `ProcessBuilder` lee la salida estándar del comando Docker.
   - Parsea el JSON resultante, elimina el directorio temporal del disco, y guarda la `Submission` en la base de datos PostgreSQL.
   - Retorna la respuesta HTTP al Frontend.

## Diagrama del Motor

```
Host Server (Java)                      Docker Container (Python:3.12-alpine)
┌────────────────────────┐              ┌───────────────────────────────────────┐
│ CodeExecutionService   │              │ /app (Read-Only Volume)               │
│                        │              │  ├── runner.py (Controlador)          │
│ 1. Crea /tmp/uuid/     │              │  ├── solution.py (Código Usuario)     │
│ 2. Escribe archivos    │ ── monta ──► │  └── test_cases.json (Inputs/Outputs) │
│ 3. docker run ...      │              │                                       │
│ 4. Espera y lee JSON   │ ◄─ stdout ── │ Ejecuta python3 runner.py             │
│ 5. Borra /tmp/uuid/    │              │                                       │
└────────────────────────┘              └───────────────────────────────────────┘
```

¡Es así de fácil, pero requiere rigor! Nunca confíes en el input del usuario. Y por favor, si alguien usa un timeout manual en vez del nativo de `subprocess`, dile que se ponga las pilas.
