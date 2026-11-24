# Seguridad del servicio de Sano Móvil

Este documento explica **qué ataques consideramos, qué hicimos para frenarlos y cómo se puede comprobar**. Está pensado para pasar casi tal cual al informe del Hito 2 (sección de seguridad y datos personales).

## 1. Conceptos básicos (en palabras simples)

| Concepto | Qué es | Cómo lo aplicamos |
|---|---|---|
| **Inyección SQL** | El atacante escribe código SQL dentro de un campo (por ejemplo, el correo) para que la base lo ejecute | Todas las consultas son **parametrizadas** (`?`): el texto del usuario viaja separado del SQL y nunca se interpreta como código. Además se validan los textos con una lista de caracteres permitidos |
| **Hash vs. cifrado** | Cifrar es reversible (con la llave se recupera el dato). Un hash no se puede revertir | Las contraseñas **no se cifran, se hashean con BCrypt** (costo 12, con sal propia). Ni nosotros podemos ver la clave de nadie |
| **Cifrado en tránsito** | Que nadie pueda leer los datos mientras viajan por la red | **HTTPS/TLS**. En desarrollo se usa HTTP en localhost; al desplegar en Render el certificado y el HTTPS los entrega la plataforma, y la conexión a la base en Neon exige SSL (`sslmode=require`) |
| **Cifrado en reposo** | Que los datos guardados en el disco no se puedan leer si roban el disco | Cifrado del disco o del volumen donde corre PostgreSQL (BitLocker en Windows, o cifrado administrado de la nube). Los datos sensibles se minimizan: no guardamos la lista de apps del usuario |
| **Autenticación vs. autorización** | Autenticar: ¿quién eres? Autorizar: ¿qué puedes hacer? | Token **JWT** firmado (autenticación) y reglas por rol y por dueño del dato (autorización) |
| **Fuerza bruta** | Probar claves una tras otra | Bloqueo de la cuenta tras 5 intentos fallidos en 15 minutos, y límite de peticiones por IP |
| **Middleware / filtro** | Código que revisa cada petición *antes* de llegar a la ruta | La validación del token la hace el plugin `Authentication` de Ktor (middleware), no un controlador que llama a otro |

## 2. Cómo se reparte la seguridad por capas

- **Presentación (rutas):** valida el token (middleware), el tamaño del cuerpo (máx. 100 KB), el formato del JSON y el rol de administrador. Traduce los errores a un formato único sin detalles internos.
- **Negocio (servicios):** valida reglas (rangos, largos, listas de valores permitidos, máximo de equipos) y aplica el bloqueo por intentos fallidos.
- **Datos (repositorios):** consultas parametrizadas y siempre filtradas por el dueño del dato (`usuario_id`).
- **Base de datos:** el servicio se conecta con el usuario `sano_app`, que **solo puede leer y escribir datos** (no crear ni borrar tablas, no ser superusuario).

## 3. Medidas implementadas (y cómo comprobarlas)

| Amenaza (OWASP API Security Top 10, 2023) | Medida | Cómo se comprueba en Postman (carpeta 6) |
|---|---|---|
| **API1 · Autorización a nivel de objeto** (ver datos de otro usuario cambiando un id) | Todas las consultas filtran por `usuario_id`; un recurso ajeno responde 404 | «A intenta LEER / BORRAR el equipo de B» → 404 |
| **API2 · Autenticación rota** | BCrypt, JWT con expiración (120 min), firma HS256, bloqueo por intentos, respuesta idéntica y de igual duración si el correo no existe | Token alterado → 401; token sin firma (`alg: none`) → 401; 5 fallos y luego 429 |
| **API3 · Exposición excesiva de datos / asignación masiva** | Se responde con DTOs explícitos: el hash de la clave nunca sale; el cliente no puede enviar `rol` ni `id` (se ignoran) | Registro → la respuesta no contiene la clave |
| **API4 · Consumo de recursos sin límite** | Límite de 300 peticiones/min por IP (20/min en login), cuerpo máximo de 100 KB, máximo de 8 equipos por usuario, listados con `limite` ≤ 100 | Superar los intentos de login → 429 |
| **API5 · Autorización a nivel de función** | Las rutas de escritura del catálogo exigen rol `admin` | Crear modelo como usuario normal → 403 |
| **API7 · Mala configuración de seguridad** | Cabeceras de seguridad, sin CORS abierto, sin trazas de error en las respuestas, el servidor no revela su tecnología | Cabeceras `X-Content-Type-Options`, `Server: sano-movil` |
| **API9 · Inventario inadecuado** | Versionado por ruta (`/api/v1`) y tabla de rutas documentada (`docs/API.md`) | — |
| **Inyección SQL** | Consultas parametrizadas + lista blanca de caracteres | Las 3 pruebas de inyección: 401, 200 sin datos y 422; las tablas siguen intactas |

## 4. Datos personales y Ley 21.719

| Exigencia del curso | Qué hacemos |
|---|---|
| Qué datos se tratan y para qué | Correo y nombre (crear y reconocer la cuenta); equipos (marca, modelo, apodo) y revisiones (puntaje, nivel, hallazgos) para mostrar el historial del equipo |
| Por qué son necesarios y no otros (minimización) | No se guarda una exportación completa de la lista de aplicaciones, el contenido de archivos, ubicación ni contactos. La app puede enviar el texto de hallazgos del análisis preventivo y ese texto puede mencionar una aplicación detectada. La contraseña no se almacena en claro. El registro de accesos guarda solo el id, la acción y la fecha (no el correo) |
| Medidas técnicas | Control de acceso por usuario y rol, contraseñas hasheadas, registro de accesos, usuario de base con permisos mínimos, HTTPS en producción, cifrado del disco |
| Qué pasa si el usuario pide eliminar sus datos | `DELETE /api/v1/auth/yo` borra la cuenta y, en cascada, sus equipos, revisiones y hallazgos |
| Datos de prueba | Todos los datos de ejemplo son ficticios |

## 5. Limitaciones (decirlas con honestidad)

- En desarrollo local el servicio corre en HTTP; el cifrado en tránsito lo aportan la plataforma de despliegue (HTTPS) y la conexión SSL a la base.
- El bloqueo por intentos protege las cuentas que existen; para el resto actúa el límite por IP (detrás de un proxy hay que activar `TRUST_PROXY=true` para ver la IP real del cliente; si se activa sin proxy, alguien podría falsear su IP).
- El token no se puede revocar antes de que expire (se mitiga con una duración corta).
- No hay verificación del correo ni doble factor de autenticación.
- Faltan revisiones automáticas de dependencias; se recomienda activar **Dependabot** y el **análisis de secretos** de GitHub (Settings > Code security).
- Registrar un correo ya existente responde 409, lo que permite saber si un correo está registrado. Es un compromiso entre seguridad y facilidad de uso.


## 6. Integración con el cliente Android (Hito 3)

La aplicación Android tiene dos modos de uso:

- **Sin cuenta:** diagnóstico, análisis preventivo y guías se ejecutan localmente. Las revisiones y equipos se conservan en el dispositivo y no se sincronizan con el backend.
- **Con cuenta:** el cliente utiliza HTTPS para registro/inicio de sesión y envía el token JWT en las solicitudes protegidas. Sincroniza los equipos del usuario y el resumen de las revisiones.

El resumen enviado por el cliente puede incluir puntaje, nivel, porcentajes disponibles de batería y espacio, y hasta 30 hallazgos con código, nivel, título y detalle. El detalle de un hallazgo puede mencionar el nombre de una aplicación que el análisis local identificó como señal de riesgo. El flujo no envía el contenido de archivos personales ni un inventario completo de las aplicaciones instaladas.

El acceso remoto se realiza con correo y contraseña. El cliente no conserva la contraseña en preferencias; el servidor almacena su hash BCrypt y emite un JWT. La app no ofrece inicio de sesión con Google ni notificaciones push en esta versión. Para el detalle de rutas y del funcionamiento del cliente, revisar [INTEGRACION_ANDROID_API.md](INTEGRACION_ANDROID_API.md).
