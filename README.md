# Sano Móvil

> **Traducimos tu celular a un lenguaje simple para que lo cuides sin miedo.**

Sano Móvil es una aplicación Android que revisa el estado del teléfono (hardware, software y privacidad) y lo explica con palabras sencillas: un puntaje de salud de 0 a 100, un semáforo de riesgos y guías paso a paso. Está pensada para adultos mayores y para personas sin conocimientos informáticos.

Proyecto del ramo **INGT1003 · Arquitectura de Desarrollo (Móvil y Web)**, Universidad Mayor. Segundo semestre 2026. Profesor: Carlos Muñoz.

## Integrantes

| Nombre | Usuario de GitHub |
|---|---|
| Matías Añasco | [@matiasAC3](https://github.com/matiasAC3) |
| Gabriel Riquelme | [@gabrielriquelme-boop](https://github.com/gabrielriquelme-boop) |

## Estado del proyecto

| Hito | Contenido | Estado |
|---|---|---|
| Hito 1 | Propuesta, prototipo de la app Android y diagramas | Entregado |
| **Hito 2** | **Servicio REST con base de datos PostgreSQL** | **Implementado en este repositorio** (carpetas `backend/` y `db/`) |
| Hito 3 | Integración de la app con el servidor | **Conexión funcional comprobada por el equipo con una APK de demostración**; se continúa documentando y preparando la entrega |

El servicio del Hito 2 se probó con una colección de **54 peticiones y 71 pruebas automáticas, todas aprobadas**, contra una base PostgreSQL real. Además, el equipo comprobó el flujo de conexión desde la aplicación Android hacia el servicio desplegado.

## Enlaces

- **Prototipo navegable (Hito 1):** https://matiasac3.github.io/sano-movil/demo.html
- **Descarga de la aplicación Android (APK):** [Versiones publicadas en GitHub Releases](https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases)
- **Documentos (PDF, diagramas e imágenes):** carpeta [`docs/`](docs/)
- **Tabla de la API (Hito 2):** [`docs/API.md`](docs/API.md)
- **Colección de pruebas (Postman):** [`docs/sano-movil.postman_collection.json`](docs/sano-movil.postman_collection.json)
- **Modelo de datos y script de creación:** carpeta [`db/`](db/)
- **Seguridad del servicio:** [`docs/SEGURIDAD.md`](docs/SEGURIDAD.md)
- **Guía de instalación paso a paso en Windows:** [`docs/INSTALACION_WINDOWS.md`](docs/INSTALACION_WINDOWS.md)
- **Despliegue gratuito en la nube:** [`docs/DESPLIEGUE_GRATIS.md`](docs/DESPLIEGUE_GRATIS.md)
- **Cómo publicar la APK:** [`docs/SUBIR_APK_GITHUB.md`](docs/SUBIR_APK_GITHUB.md)

## Qué cambió desde el Hito 1

- **Servidor definido:** API REST propia en Kotlin (Ktor) con PostgreSQL. Se descartó Firebase.
- **Base de datos relacional:** PostgreSQL, con claves, relaciones y restricciones (`db/schema.sql`).
- **Arquitectura consistente:** servidor en tres capas (presentación, negocio y datos) más el cliente Android.
- **Sesión validada con un filtro:** el token JWT lo verifica un *middleware* de autenticación antes de llegar a las rutas, y no un controlador que llama a otro.
- **Seguridad reforzada:** consultas parametrizadas, bloqueo por intentos fallidos, límite de peticiones, cabeceras de seguridad y usuario de base con permisos mínimos.
- **Cifras verificadas:** se eliminaron las que no se podían comprobar y se citan fuentes concretas.

## Tecnologías

| Pieza | Tecnología | Por qué |
|---|---|---|
| Lenguaje | Kotlin | Ya se usa en la app; un solo lenguaje en todo el proyecto |
| Servidor | Ktor | Liviano y explícito: cada ruta se lee en el código, sin anotaciones que oculten el flujo |
| Acceso a datos | JDBC con SQL explícito | Se ve exactamente qué consulta se ejecuta; todas van parametrizadas |
| Base de datos | PostgreSQL | Relacional, con restricciones e integridad referencial |
| Autenticación | JWT + BCrypt | Sesiones sin estado y contraseñas con hash |
| Arquitectura | Monolito modular | Equipo de 2 personas y carga baja; se dividiría en servicios solo si la carga lo exigiera |

## Estructura del repositorio

```
.
├── app/                 Aplicación Android (Kotlin + Jetpack Compose)
├── backend/             Servicio REST (Kotlin + Ktor + PostgreSQL)
│   └── src/main/kotlin/cl/sanomovil/backend/
│       ├── presentation/   Capa de presentación: rutas HTTP, JSON, errores, autenticación
│       ├── service/        Capa de negocio: reglas y validaciones
│       ├── repository/     Capa de datos: todo el SQL
│       ├── domain/         Entidades y errores del dominio
│       ├── security/       Hash de contraseñas y tokens JWT
│       └── config/         Lectura de variables de entorno
├── Dockerfile / docker-compose.yml   Para levantar todo con un comando o desplegar en la nube
├── db/                  Modelo de datos (schema.sql), datos de ejemplo y script de creación
├── docs/                Documentos, diagramas, API y colección de pruebas
├── .env.example         Variables de entorno de ejemplo (el .env real NO se sube)
└── README.md
```

## Descargar e instalar la APK

La APK de demostración se publica como archivo adjunto de una versión en [GitHub Releases](https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases). Abre la versión más reciente y descarga el archivo `SanoMovil.apk`. La guía para crear la publicación está en [`docs/SUBIR_APK_GITHUB.md`](docs/SUBIR_APK_GITHUB.md).

## Levantar el servicio (back-end)

Hay tres formas. **Elige una.**

### Opción A · Un solo comando con Docker (la más simple para quien evalúa)
Requiere Docker Desktop.
1. Copiar `.env.example` como `.env` y completar `DB_PASSWORD` y `JWT_SECRET` (32 o más caracteres).
2. En la raíz del repositorio: `docker compose up --build`
3. Abrir http://localhost:8080/api/v1/salud (debe decir `"estado": "ok"`).

### Opción B · En la nube, sin instalar nada
El servicio y la base pueden quedar online con planes gratuitos (Neon + Render). Paso a paso en [`docs/DESPLIEGUE_GRATIS.md`](docs/DESPLIEGUE_GRATIS.md). Desde ahí basta con importar la colección de pruebas y apuntar `baseUrl` a la dirección pública.

### Opción C · Desarrollo local
**Requisitos:** una base PostgreSQL 14 o superior (instalada en el equipo o en Neon) y Android Studio o IntelliJ IDEA con JDK 17 o superior. No hace falta instalar Gradle.
Guía con todos los pasos en [`docs/INSTALACION_WINDOWS.md`](docs/INSTALACION_WINDOWS.md).

1. **Crear la base.** Desde la raíz: `psql -U postgres -f db/crear_base.sql`
   (con pgAdmin o con el editor SQL de Neon: crear la base y ejecutar `db/schema.sql` y luego `db/seed.sql`).
2. **Variables.** Copiar `.env.example` como `.env` y completar `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET`. Si la base está en Neon, `DB_URL` termina en `?sslmode=require`.
3. **Arrancar.** En Android Studio: *File > Open* en la carpeta `backend` y ejecutar `Application.kt` (triángulo verde junto a `fun main()`). También: `cd backend` y ` .\gradlew run`.
4. **Comprobar.** http://localhost:8080/api/v1/salud

### Probar la API (cualquier opción)
Importar `docs/sano-movil.postman_collection.json` en Postman (o Bruno) y ejecutar las carpetas en orden, de la 0 a la 7. El administrador inicial se crea al arrancar con `ADMIN_EMAIL` y `ADMIN_PASSWORD` del `.env`.

## Arquitectura (resumen)

| Capa | Carpeta | Qué hace |
|---|---|---|
| Presentación | `presentation/` | Recibe la petición HTTP, la valida en formato, llama al servicio y responde con el código de estado correcto |
| Negocio | `service/` | Reglas: máximo de equipos, rangos del puntaje, permisos, validaciones |
| Datos | `repository/` | Consultas SQL parametrizadas contra PostgreSQL |

Recorrido de una petición (`POST /api/v1/equipos`): `Rutas.kt` → `EquipoService.crear` → `EquipoRepository.crear` → tabla `equipos`. El detalle completo está en el documento técnico del Hito 2.

## Seguridad y datos personales

Resumen (detalle y pruebas en [`docs/SEGURIDAD.md`](docs/SEGURIDAD.md)):

- Contraseñas con BCrypt; sesiones con token JWT con expiración; secretos en el `.env`, nunca en el código.
- Consultas SQL parametrizadas (contra inyección) y validación con lista de caracteres permitidos.
- Cada usuario solo accede a sus propios datos; las rutas de administrador exigen rol `admin`.
- Bloqueo tras 5 intentos fallidos, límite de peticiones por IP, tamaño máximo de cuerpo y cabeceras de seguridad.
- El servicio puede conectarse a la base con un usuario de **permisos mínimos** (`db/crear_usuario_app.sql`).
- Se guardan solo los datos mínimos y un registro de accesos sin el correo. El usuario puede eliminar su cuenta y sus datos con `DELETE /api/v1/auth/yo` (Ley 21.719).
- Todos los datos de ejemplo son ficticios.

## Trabajo en equipo

- Una rama por funcionalidad (`feature/...`) y unión a `main` mediante solicitud de incorporación (Pull Request) revisada por la otra persona.
- Cada integrante sube su trabajo con su propia cuenta. Ninguna credencial entra al repositorio.
