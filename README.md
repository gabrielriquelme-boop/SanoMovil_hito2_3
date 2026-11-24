# Sano Móvil
### Plataforma de diagnóstico y asistencia preventiva para Android

> **Tu celular, sano, seguro y sin complicaciones.**

Sano Móvil es una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose. Revisa el estado del teléfono y presenta los resultados en lenguaje sencillo, con recomendaciones y guías paso a paso. Está orientada especialmente a adultos mayores y personas que prefieren explicaciones claras, visuales y sin tecnicismos.

**Asignatura:** Arquitectura Web y Móvil · Universidad Mayor  
**Docente:** Carlos Muñoz  
**Integrantes:** Matías Añasco y Gabriel Riquelme  
**Entrega:** Hito 3 · noviembre de 2026

---

## Accesos principales

| Recurso | Enlace |
|---|---|
| **Descargar aplicación Android (APK)** | [SanoMovil.apk · versión 1.0.0](https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases/download/v1.0.0/SanoMovil.apk) |
| **Informe técnico final Hito 3** | [docs/Informe_Final_Hito3_Sano_Movil.pdf](docs/Informe_Final_Hito3_Sano_Movil.pdf) |
| **Manual de uso e instalación** | [docs/Manual_Usuario_Instalacion_Sano_Movil.pdf](docs/Manual_Usuario_Instalacion_Sano_Movil.pdf) |
| **Video promocional (máximo 2 minutos)** | Pendiente de incorporar enlace público |
| **Código fuente Android** | [carpeta app/](app/) |
| **Backend REST del Hito 2** | [carpeta backend/](backend/) · [API en línea](https://sanomovil-hito2-3.onrender.com/api/v1/salud) |
| **Documentación de rutas REST** | [docs/API.md](docs/API.md) |
| **Colección de pruebas Postman** | [docs/sano-movil.postman_collection.json](docs/sano-movil.postman_collection.json) |
| **Modelo y scripts PostgreSQL** | [carpeta db/](db/) |

> Los enlaces al informe final y al manual corresponden a los nombres de archivo que deben quedar dentro de `docs/`. Antes de la entrega, comprueba que ambos PDF estén cargados en esas rutas. El enlace del video debe reemplazarse por la URL pública definitiva cuando esté disponible.

## Producto funcional

La aplicación se organiza en cuatro secciones principales:

1. **Inicio y diagnóstico:** ejecuta una revisión del dispositivo y presenta un puntaje de salud de 0 a 100, junto con hallazgos priorizados y recomendaciones.
2. **Mis equipos:** muestra el teléfono anfitrión y permite registrar equipos secundarios para consultar sus fichas de referencia.
3. **Gestionar:** organiza archivos por categorías y solicita selección y confirmación antes de eliminarlos.
4. **Guías de ayuda:** ofrece 16 guías paso a paso en lenguaje cotidiano, con recursos audiovisuales integrados.

El módulo local de seguridad identifica señales preventivas de riesgo y analiza archivos APK sin instalarlos. **No es un antivirus**: sus resultados son heurísticos y deben interpretarse como orientación preventiva.

### Flujo principal de uso

1. Instalar y abrir Sano Móvil en un dispositivo Android.
2. Elegir **Continuar sin cuenta** para entrar al modo local.
3. En **Inicio**, pulsar **Escanear**.
4. Revisar el puntaje y los hallazgos priorizados.
5. Abrir la guía sugerida para consultar los pasos de solución.

## Requisitos de instalación

- Android 8.0 (API 26) o superior.
- Para utilizar el gestor de archivos **Gestionar**, se requiere Android 11 (API 30) o superior y conceder el permiso de acceso a todos los archivos cuando el sistema lo solicite.
- Se recomiendan al menos 50 MB de espacio libre.
- El diagnóstico local y las guías no requieren conexión a Internet.

Los pasos completos, permisos, casos de prueba y solución de fallas frecuentes están en el [Manual de uso e instalación](docs/Manual_Usuario_Instalacion_Sano_Movil.pdf).

## Arquitectura de la solución

El informe final del Hito 3 describe cuatro capas desacopladas:

| Capa | Responsabilidad |
|---|---|
| Presentación | Interfaz Android y navegación con Jetpack Compose. |
| Lógica local y seguridad | Diagnóstico de hardware, auditoría de permisos, análisis preventivo y gestión de archivos. |
| Servicios y autenticación | Contrato `ProveedorAutenticacion` con proveedor de demostración local y extensión preparada para autenticación remota. |
| Datos | Catálogos JSON en `assets/datos/`, videos en `res/raw/` y persistencia local. |

### Tecnologías

- **Cliente Android:** Kotlin, Jetpack Compose y APIs nativas de Android.
- **Contenido local:** archivos JSON validados y recursos multimedia.
- **Backend complementario del Hito 2:** Kotlin, Ktor, JDBC y PostgreSQL.
- **Autenticación del backend:** JWT y BCrypt.

### Estado de conectividad

La versión base descrita en el informe final del Hito 3 prioriza el funcionamiento local (*offline-first*). El informe declara que la autenticación remota y las notificaciones push no están activas por defecto. El backend REST del Hito 2 permanece en el repositorio como servicio complementario y se puede revisar por separado mediante su endpoint de salud y la colección Postman.

**Importante para la revisión:** el manual contiene una explicación de sincronización opcional con Render, mientras que el informe final Hito 3 y el `AndroidManifest.xml` actual describen una compilación base sin permiso `INTERNET`. Por ello, no se debe asumir que la APK publicada sincroniza con el backend en esta configuración. La documentación y el permiso del manifiesto deben mantenerse alineados con la APK exacta que se entregue.

## Backend REST y persistencia (Hito 2)

El repositorio conserva el servicio REST desarrollado con Kotlin y Ktor, organizado en capas de presentación, negocio y datos. Utiliza PostgreSQL y consultas JDBC parametrizadas.

- **Comprobación del servicio:** [GET /api/v1/salud](https://sanomovil-hito2-3.onrender.com/api/v1/salud).
- **Contrato de la API:** [docs/API.md](docs/API.md).
- **Pruebas de API:** importar [la colección Postman](docs/sano-movil.postman_collection.json).
- **Base de datos:** revisar `db/schema.sql`, `db/seed.sql` y `db/crear_base.sql`.

Para levantar el backend localmente se puede utilizar Docker Compose:

```bash
docker compose up --build
```

Se debe crear un archivo local `.env` a partir de `.env.example` y completar las variables requeridas. **El archivo `.env` real no debe subirse al repositorio.** Los pasos alternativos están en [docs/INSTALACION_WINDOWS.md](docs/INSTALACION_WINDOWS.md) y [docs/DESPLIEGUE_GRATIS.md](docs/DESPLIEGUE_GRATIS.md).

## Evolución, decisiones y limitaciones

La entrega final registra la evolución desde el Hito 1, la matriz de trazabilidad, la bitácora de decisiones de arquitectura (ADR) y los incidentes técnicos detectados y resueltos durante la integración.

Limitaciones declaradas del producto actual:

- El diagnóstico real se ejecuta sobre el teléfono anfitrión; los equipos secundarios agregados manualmente muestran fichas de referencia.
- La detección de amenazas es heurística y no reemplaza un antivirus.
- El gestor de archivos está condicionado por la versión de Android y sus permisos de almacenamiento.
- La aplicación es exclusiva para Android.
- La autenticación remota y las notificaciones push no están activas en la configuración base descrita para Hito 3.

El informe final también contiene la declaración de uso de inteligencia artificial, la verificación humana del resultado y el análisis de datos personales y privacidad.

## Estructura del repositorio

```
.
├── app/                   Aplicación Android (Kotlin + Jetpack Compose)
├── backend/               Servicio REST Kotlin + Ktor
├── db/                    Esquema, datos de ejemplo y scripts PostgreSQL
├── docs/                  Informes, manual, API y colección de pruebas
├── .env.example           Variables de entorno de ejemplo
├── Dockerfile             Imagen del backend
├── docker-compose.yml     Backend y PostgreSQL local
└── README.md              Instrucciones y enlaces de entrega
```

## Documentos de la entrega

- **Informe final Hito 3:** arquitectura, alcance funcional, diagramas, evolución, decisiones, incidentes, limitaciones, bibliografía, declaración de IA y privacidad.
- **Manual de uso e instalación:** requisitos, permisos, instalación de APK, primer acceso, uso por secciones, escenarios de prueba y resolución de fallas.
- **Informe técnico Hito 2:** [Documento_Tecnico_Hito2.pdf](docs/Documento_Tecnico_Hito2.pdf), como antecedente detallado del backend, API y persistencia.

## Video promocional

**Pendiente:** agregar aquí el enlace público del video promocional (duración máxima de 2:00), comprobando que pueda visualizarse sin solicitar permisos de acceso.
