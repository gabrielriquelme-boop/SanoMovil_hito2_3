# Integración Android - API REST (Hito 3)

**Versión de referencia:** APK SanoMovil v1.0.0 y proyecto Android entregado por el equipo  
**Fecha de actualización:** 24 de noviembre de 2026  
**Servidor:** `https://sanomovil-hito2-3.onrender.com/api/v1`

## Resumen

Sano Móvil conserva el diagnóstico y el análisis preventivo en el dispositivo Android. Cuando la persona elige crear una cuenta o iniciar sesión con correo y contraseña, la app utiliza la API REST del Hito 2 para autenticar la cuenta y sincronizar equipos y revisiones.

La opción **Continuar sin cuenta** mantiene el uso local. En ese modo no se realizan las llamadas autenticadas de sincronización.

## Componentes del cliente

| Archivo | Responsabilidad |
|---|---|
| `AndroidManifest.xml` | Declara `INTERNET` y `allowBackup=false`. |
| `Api.kt` | Cliente HTTP/HTTPS, solicitudes JSON, token JWT, errores, equipos e historial. |
| `Autenticacion.kt` | Selecciona `AutenticacionApi` como proveedor activo; conserva `AutenticacionDemo` como implementación de demostración alternativa. |
| `MainActivity.kt` | Inicializa el cliente API, gestiona la sesión y envía el resumen de una revisión cuando existe token. |
| `Ecosistema.kt` | Conserva la lista local cuando no hay cuenta y mantiene una copia sincronizada cuando hay sesión. |
| `PantallaEcosistema.kt` | Permite consultar equipos, agregar/quitar equipos y ver el historial del teléfono anfitrión. |
| `PantallaLogin.kt` | Interfaz de registro e inicio de sesión mediante correo y contraseña. |

## Flujo con cuenta

1. La persona crea su cuenta o inicia sesión con correo y contraseña.
2. El backend responde con un token JWT; el cliente lo conserva en preferencias privadas y lo envía como `Authorization: Bearer <token>` en las solicitudes protegidas.
3. La sección **Mis equipos** consulta la lista de equipos de la cuenta y permite agregar o quitar registros.
4. El diagnóstico del teléfono anfitrión se ejecuta localmente.
5. Al terminar el escaneo, si hay token, la app intenta enviar el resumen de la revisión al servidor.
6. La ficha **Este teléfono** puede consultar las últimas diez revisiones guardadas.

El modo sin cuenta sigue permitiendo el diagnóstico local, las guías y el uso de las funciones que no requieren autenticación remota.

## Rutas REST utilizadas

| Método | Ruta | Uso desde Android |
|---|---|---|
| POST | `/auth/registro` | Crear una cuenta. Después del registro, el cliente inicia sesión para obtener el token. |
| POST | `/auth/login` | Iniciar sesión y recibir el token JWT. |
| GET | `/equipos` | Consultar equipos de la cuenta. |
| POST | `/equipos` | Crear un equipo o el registro que representa al teléfono anfitrión. |
| DELETE | `/equipos/{id}` | Eliminar un equipo de la cuenta. |
| POST | `/equipos/{id}/revisiones` | Guardar un resumen de revisión y sus hallazgos. |
| GET | `/equipos/{id}/revisiones?limite=10` | Consultar las últimas diez revisiones del teléfono anfitrión. |

El contrato completo, ejemplos JSON y códigos de estado están en [API.md](API.md).

## Datos personales y minimización

Cuando se usa una cuenta, el cliente envía:

- **Cuenta:** correo y contraseña para autenticar el acceso; durante el registro también envía un nombre derivado del correo.
- **Equipos:** tipo, marca, modelo y apodo de los equipos que registra la persona.
- **Resumen de revisión:** puntaje, nivel, porcentajes de batería y espacio libre cuando están disponibles, y hasta 30 hallazgos con código, nivel, título y detalle.

El texto de un hallazgo puede mencionar una aplicación detectada como señal de riesgo. El cliente no envía el contenido de archivos personales ni una exportación completa del inventario de aplicaciones instaladas mediante el flujo de sincronización. La contraseña se transmite por HTTPS para el registro/inicio de sesión y el backend almacena un hash BCrypt. El cliente no guarda la contraseña en preferencias; conserva el token de sesión.

Al cerrar sesión, el cliente elimina el token y limpia la copia local de equipos sincronizados. El backend conserva los registros de cuenta, equipos y revisiones en PostgreSQL asociados al usuario autenticado.

## Límites declarados

- El escaneo real de hardware, permisos y seguridad se ejecuta en el teléfono donde está instalada la app.
- Los equipos secundarios se almacenan en la cuenta y se muestran como fichas de referencia; no se escanean a distancia desde el teléfono anfitrión.
- El acceso con Google no está disponible en esta versión. El acceso remoto utiliza correo y contraseña.
- No hay notificaciones push.
- La detección de amenazas es heurística; no sustituye un antivirus.
- La sincronización requiere Internet y disponibilidad del servicio Render. La primera solicitud puede tardar hasta cerca de un minuto si la instancia gratuita estaba inactiva; el cliente configura un tiempo de lectura de hasta 75 segundos.

## Comprobación funcional sugerida

Para la demostración, utilizar una cuenta de prueba y datos ficticios:

1. Abrir [la ruta de salud](https://sanomovil-hito2-3.onrender.com/api/v1/salud) y confirmar que responde.
2. En la app, crear una cuenta de prueba o iniciar sesión con una cuenta ya creada.
3. Abrir **Mis equipos** y comprobar que se carga la lista desde el servidor.
4. Agregar un equipo ficticio y comprobar que aparece al volver a sincronizar.
5. Ejecutar **Escanear** en el teléfono anfitrión.
6. Abrir **Mis equipos > Este teléfono** y comprobar que aparece la revisión en el historial.

No utilizar datos personales reales en la demostración académica.
