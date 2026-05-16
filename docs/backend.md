# Documentación del Backend (Spring Boot)

¡Locura cósmica! El backend es el cerebro y el escudo protector de nuestra aplicación. Aquí no confiamos en NADA de lo que manda el cliente.

## Estructura de Paquetes

Seguimos una arquitectura en capas clásica pero enfocada al dominio:

- `config/`: Configuraciones de Beans (CORS, RestTemplate, propiedades del motor de ejecución).
- `controller/`: La capa REST. ¡Aquí NO VA LA LÓGICA DE NEGOCIO! Solo validamos inputs y llamamos al servicio.
- `dto/`: Data Transfer Objects. Lo que sale de la DB no es lo mismo que lo que le mandamos al cliente. Separar modelos de DTOs es vital para no exponer contraseñas u ocultar relaciones cíclicas.
- `model/`: Entidades JPA. La representación de nuestras tablas.
- `repository/`: Interfaces de Spring Data JPA.
- `security/`: Filtros JWT, UserDetailsService, y el EntryPoint de acceso denegado.
- `service/`: **AQUÍ ESTÁ LA MAGIA.** La lógica de negocio pura y dura. Transactional y agnóstica de HTTP.

## Seguridad (JWT)

Usamos autenticación sin estado (Stateless). 
1. El usuario hace `/auth/login` o `/auth/register`.
2. Verificamos credenciales con `AuthenticationManager`.
3. Generamos un token JWT firmado con nuestro secreto (`JwtTokenProvider`).
4. Cada petición subsiguiente pasa por `JwtAuthenticationFilter`, donde extraemos el Bearer token, lo validamos, y metemos el usuario en el `SecurityContextHolder`.

*Regla de oro:* Jamás almacenes el secreto del JWT en el código fuente en producción, usa variables de entorno.

## Manejo Global de Excepciones

No queremos estar tirando `try-catch` en cada controlador. Para eso existe `GlobalExceptionHandler` con la anotación `@ControllerAdvice`.
- Convertimos `ResourceNotFoundException` a un hermoso JSON con status 404.
- Convertimos `BadCredentialsException` a status 401.
- Errores genéricos a 500 sin exponer la traza del stack al frontend.

## Migraciones con Flyway

El esquema de la base de datos vive en `src/main/resources/db/migration/`.
Cada archivo tiene un formato `V{Numero}__{Descripcion}.sql`. 
- Una vez que un archivo se sube, **NUNCA SE MODIFICA**. Si te equivocaste, creas uno nuevo (`V4...`) alterando la tabla. Así es como los profesionales mantienen la consistencia de datos en todos los entornos.
