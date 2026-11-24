# Sano Móvil
### Plataforma Android de diagnóstico preventivo y asistencia para el cuidado del celular

> **Tu celular, sano, seguro y sin complicaciones.**

Sano Móvil es una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose. Examina el estado del teléfono anfitrión y explica los resultados con lenguaje sencillo, recomendaciones priorizadas y guías paso a paso. Está orientada especialmente a adultos mayores y personas que prefieren explicaciones claras, visuales y sin tecnicismos.

**Asignatura:** Arquitectura Web y Móvil · Universidad Mayor  
**Docente:** Carlos Muñoz  
**Integrantes:** Matías Añasco y Gabriel Riquelme  
**Entrega:** Hito 3 · noviembre de 2026

---

## Entregables y accesos

| Recurso | Enlace |
|---|---|
| **Aplicación Android (APK)** | [Descargar SanoMovil.apk · versión 1.0.0](https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases/download/v1.0.0/SanoMovil.apk) |
| **Informe técnico final Hito 3** | [docs/Informe_Final_Hito3_Sano_Movil.pdf](docs/Informe_Final_Hito3_Sano_Movil.pdf) |
| **Manual de uso e instalación** | [docs/Manual_Usuario_Instalacion_Sano_Movil.pdf](docs/Manual_Usuario_Instalacion_Sano_Movil.pdf) |
| **Video promocional (máximo 2 minutos)** | https://youtube.com/shorts/FsBigcUj5UM |
| **Código fuente Android** | [carpeta app/](app/) |
| **Backend REST del Hito 2** | [carpeta backend/](backend/) · [comprobación de salud](https://sanomovil-hito2-3.onrender.com/api/v1/salud) |
| **Contrato de la API** | [docs/API.md](docs/API.md) |
| **Colección de pruebas Postman** | [docs/sano-movil.postman_collection.json](docs/sano-movil.postman_collection.json) |
| **Modelo de datos y scripts PostgreSQL** | [carpeta db/](db/) |
| **Seguridad del backend** | [docs/SEGURIDAD.md](docs/SEGURIDAD.md) |

Los PDF del informe final y el manual deben estar guardados en las rutas indicadas dentro de `docs/`. El enlace del video se incorporará cuando el archivo esté publicado con acceso público.

## Producto funcional

La aplicación se organiza en cuatro secciones principales:

1. **Inicio y diagnóstico:** ejecuta una revisión local del dispositivo anfitrión y presenta un puntaje de salud de 0 a 100, hallazgos priorizados y recomendaciones.
2. **Mis equipos:** permite mantener una lista de equipos. Con una cuenta, la lista se sincroniza con el backend; los equipos secundarios muestran fichas de referencia y no se escanean en vivo desde el teléfono anfitrión.
3. **Gestionar:** organiza archivos por categorías y solicita selección y confirmación antes de eliminarlos.
4. **Guías de ayuda:** ofrece guías paso a paso en lenguaje cotidiano, con recursos audiovisuales integrados localmente.

El módulo de seguridad analiza señales preventivas locales y archivos APK sin instalarlos. **No es un antivirus**: su evaluación es heurística y debe interpretarse como orientación preventiva, no como una garantía de ausencia de amenazas.

### Flujo principal de uso

1. Instalar y abrir Sano Móvil en un dispositivo Android.
2. Elegir **Continuar sin cuenta** para utilizar las funciones locales, o crear/iniciar sesión con correo y contraseña para activar la cuenta del servidor.
3. En **Inicio**, pulsar **Escanear**.
4. Revisar el puntaje y los hallazgos priorizados.
5. Abrir la guía sugerida para consultar los pasos de solución.
6. Con una sesión iniciada, la aplicación envía el resumen de la revisión al backend y permite consultar el historial de revisiones desde **Mis equipos**.

## Requisitos de instalación

- Android 8.0 (API 26) o superior.
- Para utilizar el gestor de archivos **Gestionar**, se requiere Android 11 (API 30) o superior y conceder el permiso de acceso a todos los archivos cuando el sistema lo solicite.
- Se recomiendan al menos 50 MB de espacio libre.
- El diagnóstico, la seguridad preventiva local y las guías funcionan sin conexión.
- Se requiere conexión a Internet para registrar/iniciar sesión y sincronizar equipos e historial con el backend.

Los pasos completos, permisos, casos de prueba y solución de fallas frecuentes están en el [Manual de uso e instalación](docs/Manual_Usuario_Instalacion_Sano_Movil.pdf).

## Arquitectura de la solución

La implementación combina cuatro capas en el cliente Android y un backend REST complementario:

| Capa | Responsabilidad |
|---|---|
| Presentación | Interfaz Android y navegación con Jetpack Compose. |
| Lógica local y seguridad | Diagnóstico del dispositivo, auditoría de permisos, análisis preventivo y gestión de archivos. |
| Servicios y autenticación | `Api.kt` centraliza las solicitudes HTTPS; `AutenticacionApi` gestiona registro, inicio de sesión y token JWT. |
| Datos | Catálogos JSON y videos locales; almacenamiento privado en el dispositivo y persistencia remota PostgreSQL para cuentas, equipos y revisiones. |

### Tecnologías

- **Cliente Android:** Kotlin, Jetpack Compose y APIs nativas de Android.
- **Cliente HTTP:** `HttpURLConnection` sobre HTTPS, con solicitudes JSON y token JWT Bearer.
- **Backend:** Kotlin y Ktor, con capas de presentación, negocio y datos.
- **Persistencia remota:** PostgreSQL mediante JDBC y consultas parametrizadas.
- **Autenticación:** correo y contraseña validados por el backend; las contraseñas se almacenan como hash BCrypt y la sesión se representa con un token JWT.
- **Contenido educativo:** catálogos JSON validados en `app/src/main/assets/datos/` y videos esquemáticos en `app/src/main/res/raw/`.

## Conectividad: modo local y modo con cuenta

Sano Móvil mantiene las funciones principales de diagnóstico en el dispositivo. La conexión al backend se activa cuando la persona crea una cuenta o inicia sesión con correo y contraseña.

**Sin cuenta**
- El diagnóstico, los hallazgos, las guías y el uso del gestor de archivos se realizan localmente.
- La revisión resumida se conserva en el almacenamiento privado del teléfono.
- Los equipos añadidos se mantienen como una lista local.
- No se envía el resumen de revisión al servidor.

**Con cuenta**
- El registro y el inicio de sesión se realizan contra la API REST publicada en Render.
- La aplicación conserva el token JWT para autorizar las solicitudes posteriores.
- La lista de equipos se consulta y sincroniza con el backend.
- Al terminar un escaneo, la aplicación intenta guardar en el servidor el resumen de la revisión y sus hallazgos.
- En la ficha de **Este teléfono** se pueden consultar las últimas revisiones guardadas en la cuenta.

La integración utiliza la dirección base `https://sanomovil-hito2-3.onrender.com/api/v1`. La autenticación con Google no está disponible en esta versión; el acceso remoto se realiza con correo y contraseña. El backend no implementa notificaciones push.

## Datos personales y privacidad

Cuando se utiliza el modo sin cuenta, las funciones de diagnóstico se ejecutan localmente. Cuando se utiliza una cuenta, la aplicación transmite al backend, mediante HTTPS, los datos necesarios para operar la cuenta y sincronizarla: correo y nombre de perfil, datos declarados de los equipos (tipo, marca, modelo y apodo) y el resumen de las revisiones (puntaje, nivel, porcentajes disponibles de batería/espacio y hallazgos). El backend conserva los datos de cuenta, equipos y revisiones asociados al usuario.

La aplicación no envía al backend el contenido de los archivos personales ni el listado de aplicaciones instaladas como parte del resumen de revisión. La contraseña se envía para autenticar el acceso y el backend la almacena mediante BCrypt; la aplicación no la conserva en sus preferencias. El manifiesto de Android establece `allowBackup="false"` para evitar copias de seguridad automáticas de los datos privados de la aplicación.

La detección de amenazas es heurística y no reemplaza herramientas de seguridad especializadas. El diagnóstico real solo se ejecuta en el teléfono donde está instalada la aplicación.

## Backend REST y persistencia (Hito 2)

El repositorio conserva el servicio REST desarrollado con Kotlin y Ktor, organizado en capas de presentación, negocio y datos. Utiliza PostgreSQL y consultas JDBC parametrizadas.

- **Comprobación del servicio:** [GET /api/v1/salud](https://sanomovil-hito2-3.onrender.com/api/v1/salud).
- **Contrato de la API:** [docs/API.md](docs/API.md).
- **Pruebas:** importar [la colección Postman](docs/sano-movil.postman_collection.json).
- **Base de datos:** revisar `db/schema.sql`, `db/seed.sql` y `db/crear_base.sql`.

Para levantar el backend localmente se puede utilizar Docker Compose:

```bash
docker compose up --build
```

Se debe crear un archivo local `.env` a partir de `.env.example` y completar las variables requeridas. **El archivo `.env` real no debe subirse al repositorio.** Los pasos alternativos están en [docs/INSTALACION_WINDOWS.md](docs/INSTALACION_WINDOWS.md) y [docs/DESPLIEGUE_GRATIS.md](docs/DESPLIEGUE_GRATIS.md).

## Evolución, decisiones y limitaciones

La entrega final registra la evolución desde el Hito 1, la matriz de trazabilidad, la bitácora de decisiones de arquitectura (ADR) y los incidentes técnicos detectados y resueltos durante la integración.

Limitaciones declaradas del producto actual:

- El diagnóstico real se ejecuta sobre el teléfono anfitrión; los equipos secundarios agregados manualmente muestran fichas de referencia y no se escanean en vivo desde ese teléfono.
- La detección de amenazas es heurística y puede producir falsos positivos; no reemplaza un antivirus.
- El gestor de archivos está condicionado por la versión de Android y sus permisos de almacenamiento.
- La aplicación es exclusiva para Android.
- El acceso remoto requiere Internet y cuenta; la opción de Google y las notificaciones push no están disponibles en esta versión.

El informe final incluye la declaración de uso de inteligencia artificial, la verificación humana del resultado y el análisis de datos personales y privacidad.

## Estructura del repositorio

```
.
├── app/                   Aplicación Android (Kotlin + Jetpack Compose)
├── backend/               Servicio REST Kotlin + Ktor
├── db/                    Esquema, datos de ejemplo y scripts PostgreSQL
├── docs/                  Informes, manual, API y colección de pruebas
├── .env.example           Variables de entorno de ejemplo
├── gradle.properties      Configuración común de Gradle
├── Dockerfile             Imagen del backend
├── docker-compose.yml     Backend y PostgreSQL local
└── README.md              Información y enlaces de entrega
```

## Documentos de la entrega

- **Informe final Hito 3:** alcance funcional, arquitectura, conectividad, datos personales, evolución, decisiones, incidentes, limitaciones y bibliografía.
- **Manual de uso e instalación:** requisitos, permisos, instalación de APK, primer acceso, modo sin cuenta, cuenta remota, sincronización y solución de fallas.
- **Informe técnico Hito 2:** [Documento_Tecnico_Hito2.pdf](docs/Documento_Tecnico_Hito2.pdf), como antecedente detallado del backend, API y persistencia.
