# API REST de Sano Móvil · versión 1

**URL base:** `http://localhost:8080/api/v1`
**Formato:** JSON (UTF-8). Los nombres de los campos van en `snake_case`.
**Versionado:** por ruta (`/api/v1`). Un cambio que rompa el contrato saldría como `/api/v2`, y `v1` seguiría funcionando.
**Autenticación:** token JWT en la cabecera `Authorization: Bearer <token>`. Se obtiene en `POST /auth/login`. Dura 120 minutos (configurable).

## Formato de error (siempre el mismo)
```json
{ "error": { "codigo": "VALIDACION_FALLIDA", "mensaje": "Los datos enviados no son válidos", "detalles": ["puntaje: debe estar entre 0 y 100"] } }
```

## Códigos de estado que usa la API
| Código | Cuándo |
|---|---|
| 200 OK | Lectura o actualización correcta |
| 201 Created | Se creó un recurso (incluye cabecera `Location`) |
| 204 No Content | Se borró correctamente (sin cuerpo) |
| 400 Bad Request | JSON mal formado, faltan campos obligatorios o un parámetro de ruta no es un entero positivo |
| 401 Unauthorized | Falta el token, es inválido o expiró; o correo/clave incorrectos |
| 403 Forbidden | El token es válido pero no tiene permiso (se requiere rol `admin`) |
| 404 Not Found | El recurso no existe **o no pertenece al usuario** (no se revela cuál de los dos) |
| 409 Conflict | Choque con el estado actual: correo ya registrado, modelo duplicado, máximo de 8 equipos |
| 422 Unprocessable Entity | El JSON es válido pero los datos no cumplen las reglas (rangos, largos, valores permitidos) |
| 503 Service Unavailable | `/salud` cuando la base de datos no responde |
| 413 Payload Too Large | El cuerpo supera 100 KB |
| 429 Too Many Requests | 5 inicios de sesión fallidos en 15 minutos, o se superó el límite de peticiones por minuto |
| 500 Internal Server Error | Error inesperado (el detalle solo se escribe en el registro del servidor) |

## Tabla de rutas
| Método | Ruta | Auth | Descripción | Éxito | Errores |
|---|---|---|---|---|---|
| GET | `/salud` | No | Estado del servicio y de la base | 200 | 503 |
| POST | `/auth/registro` | No | Crea una cuenta | 201 | 400, 409, 422 |
| POST | `/auth/login` | No | Inicia sesión y entrega el token | 200 | 400, 401, 429 |
| GET | `/auth/yo` | Sí | Datos de la cuenta actual | 200 | 401, 404 |
| DELETE | `/auth/yo` | Sí | Elimina la cuenta y todos sus datos | 204 | 401, 404 |
| GET | `/equipos?tipo=` | Sí | Lista los equipos del usuario (filtro opcional por tipo) | 200 | 400, 401 |
| POST | `/equipos` | Sí | Agrega un equipo | 201 | 400, 401, 409, 422 |
| GET | `/equipos/{id}` | Sí | Detalle de un equipo | 200 | 400, 401, 404 |
| PUT | `/equipos/{id}` | Sí | Reemplaza los datos de un equipo | 200 | 400, 401, 404, 422 |
| DELETE | `/equipos/{id}` | Sí | Borra un equipo y sus revisiones | 204 | 400, 401, 404 |
| GET | `/equipos/{id}/revisiones?limite=` | Sí | Historial del equipo (más recientes primero; `limite` 1–100, por defecto 20) | 200 | 400, 401, 404 |
| POST | `/equipos/{id}/revisiones` | Sí | Guarda una revisión con sus hallazgos | 201 | 400, 401, 404, 422 |
| GET | `/revisiones/{id}` | Sí | Detalle de una revisión con hallazgos | 200 | 400, 401, 404 |
| PATCH | `/revisiones/{id}` | Sí | Cambia solo la nota de la revisión | 200 | 400, 401, 404, 422 |
| DELETE | `/revisiones/{id}` | Sí | Borra una revisión | 204 | 400, 401, 404 |
| GET | `/modelos?marca=` | Sí | Catálogo de modelos (filtro opcional por marca) | 200 | 401 |
| GET | `/modelos/{id}` | Sí | Detalle de un modelo | 200 | 400, 401, 404 |
| POST | `/modelos` | Admin | Agrega un modelo al catálogo | 201 | 400, 401, 403, 409, 422 |
| PUT | `/modelos/{id}` | Admin | Reemplaza un modelo | 200 | 400, 401, 403, 404, 409, 422 |
| DELETE | `/modelos/{id}` | Admin | Borra un modelo | 204 | 400, 401, 403, 404 |

## Ejemplos de cuerpo

**POST /auth/registro** (campos: `correo`, `nombre`, `clave`)
```json
{ "correo": "ana@ejemplo.cl", "nombre": "Ana Pérez", "clave": "Prueba12345" }
```
Respuesta 201:
```json
{ "id": 2, "correo": "ana@ejemplo.cl", "nombre": "Ana Pérez", "rol": "usuario", "creado_en": "2026-10-30T14:02:11.482Z" }
```
Reglas: correo válido; nombre de 2 a 60 caracteres; clave de 8 a 72 caracteres con al menos una letra y un número. La clave nunca se devuelve y se guarda como hash BCrypt.

**POST /auth/login** (campos: `correo`, `clave`) → 200
```json
{ "token": "eyJhbGciOi...", "tipo": "Bearer", "expira_en_seg": 7200,
  "usuario": { "id": 2, "correo": "ana@ejemplo.cl", "nombre": "Ana Pérez", "rol": "usuario", "creado_en": "2026-10-30T14:02:11.482Z" } }
```

**POST /equipos** (campos: `tipo` = telefono | tablet | otro, `marca`, `modelo`, `apodo` opcional, `modelo_id` opcional)
```json
{ "tipo": "telefono", "marca": "EjemploMarca", "modelo": "EJEMPLO-NO-REAL-1", "apodo": "Celular de mamá", "modelo_id": 1 }
```
Respuesta 201 (cabecera `Location: /api/v1/equipos/1`):
```json
{ "id": 1, "tipo": "telefono", "marca": "EjemploMarca", "modelo": "EJEMPLO-NO-REAL-1", "apodo": "Celular de mamá", "modelo_id": 1, "creado_en": "2026-10-30T14:05:40.113Z" }
```
Reglas: `marca` y `apodo` hasta 40 caracteres, `modelo` hasta 60, solo letras, números, espacios, punto y guion. Máximo 8 equipos por usuario (409 `LIMITE_EQUIPOS`).

**POST /equipos/{id}/revisiones** (campos: `puntaje` 0–100, `nivel` = ok | atencion | peligro, `bateria_pct`, `espacio_libre_pct`, `nota`, `hallazgos`)
```json
{ "puntaje": 82, "nivel": "atencion", "bateria_pct": 76, "espacio_libre_pct": 38, "nota": "Revisión de prueba",
  "hallazgos": [ { "codigo": "parche_seguridad", "nivel": "atencion", "titulo": "Actualizaciones de seguridad",
                   "detalle": "La última actualización es del 5 de mayo." } ] }
```
Respuesta 201:
```json
{ "id": 1, "equipo_id": 1, "puntaje": 82, "nivel": "atencion", "bateria_pct": 76, "espacio_libre_pct": 38,
  "nota": "Revisión de prueba", "creado_en": "2026-10-30T14:08:02.771Z",
  "hallazgos": [ { "id": 1, "codigo": "parche_seguridad", "nivel": "atencion", "titulo": "Actualizaciones de seguridad", "detalle": "La última actualización es del 5 de mayo." } ] }
```

**PATCH /revisiones/{id}** (campo: `nota`; `null` borra la nota)
```json
{ "nota": "Se instaló la actualización" }
```

**POST /modelos** (solo admin; campos: `marca`, `nombre`, `anio_lanzamiento` opcional, `notas` opcional)
```json
{ "marca": "EjemploMarca", "nombre": "EJEMPLO-NO-REAL-3", "anio_lanzamiento": 2025, "notas": "Modelo ficticio" }
```

**GET /salud** → 200
```json
{ "estado": "ok", "base_de_datos": "ok", "version": "0.2.0" }
```

## Seguridad aplicada
- Contraseñas con BCrypt (costo 12); nunca se devuelven.
- Cada usuario solo ve y modifica **sus** equipos y revisiones (todas las consultas filtran por el dueño).
- Consultas SQL parametrizadas (sin concatenar texto del usuario).
- Credenciales y secreto del token fuera del repositorio (`.env`).
- Registro de accesos (registro, inicio de sesión correcto o fallido, eliminación de cuenta).
- Bloqueo tras 5 intentos fallidos en 15 minutos, límite de peticiones por IP, cuerpo máximo de 100 KB y cabeceras de seguridad.

Detalle completo en [`SEGURIDAD.md`](SEGURIDAD.md).
