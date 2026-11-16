# Poner el servicio en internet gratis (tu PC no es el servidor)

Usamos dos servicios gratuitos que **no piden tarjeta de crédito**:

| Pieza | Servicio | Qué hace | Límites del plan gratuito |
|---|---|---|---|
| Base de datos | **Neon** (neon.com) | PostgreSQL en la nube | Plan gratuito permanente, 0.5 a 1 GB; se "duerme" tras 5 minutos sin uso y la primera consulta tarda unos segundos en despertarla |
| Servicio (API) | **Render** (render.com) | Ejecuta nuestro servicio en la nube con HTTPS | Se duerme tras 15 minutos sin tráfico y tarda cerca de 1 minuto en despertar; 512 MB de RAM; la base gratuita de Render expira a los 30 días, por eso la base va en Neon |

> Estos planes sirven para el curso. Un servicio real, con usuarios, usaría planes de pago (en nuestro modelo financiero, entre US$13 y US$45 al mes de infraestructura).

## Parte 1 · Base de datos en Neon
1. Entra a https://neon.com, crea una cuenta (con GitHub o correo) y crea un proyecto llamado `sano-movil`. Elige la región más cercana a Chile que aparezca.
2. En el panel del proyecto abre **Connection Details** y anota: *host* (algo como `ep-xxxx.region.aws.neon.tech`), *base de datos* (`neondb`), *usuario* y *contraseña*.
3. Abre **SQL Editor**, pega todo el contenido de `db/schema.sql` y ejecuta (**Run**). Luego pega `db/seed.sql` y ejecuta de nuevo.
4. Comprueba en **Tables** que aparezcan 6 tablas.
5. Arma tus variables:
   - `DB_URL=jdbc:postgresql://TU-HOST/neondb?sslmode=require`
   - `DB_USER=` el usuario que te dio Neon
   - `DB_PASSWORD=` la contraseña que te dio Neon
   Si la cadena de conexión de Neon trae `channel_binding=require`, **no la copies** a `DB_URL`.

**Ventaja extra:** puedes desarrollar desde tu PC apuntando a esta base de Neon (en tu `.env` local) y así **no necesitas instalar PostgreSQL**.

## Parte 2 · Servicio en Render
1. El repositorio debe estar en GitHub con el archivo `Dockerfile` en la raíz (ya viene en el proyecto).
2. En https://render.com crea una cuenta con GitHub. Pulsa **New > Web Service** y elige tu repositorio.
3. Configura:
   - **Language:** Docker (usa el `Dockerfile` de la raíz)
   - **Instance Type:** Free
   - **Health Check Path:** `/api/v1/salud`
4. En **Environment Variables** agrega (estas claves viven en Render, NO en GitHub):

   | Variable | Valor |
   |---|---|
   | `DB_URL` | la de Neon (con `?sslmode=require`) |
   | `DB_USER` | usuario de Neon |
   | `DB_PASSWORD` | contraseña de Neon |
   | `JWT_SECRET` | un texto largo y aleatorio de 32 o más caracteres (distinto al de tu PC) |
   | `ADMIN_EMAIL` | el correo del administrador |
   | `ADMIN_PASSWORD` | **una clave larga y propia** (nunca la del ejemplo: el servicio queda público) |
   | `TRUST_PROXY` | `true` |

   Render define `PORT` por su cuenta.
5. Pulsa **Create Web Service**. La primera compilación tarda varios minutos.
6. Al terminar, tu servicio queda en una dirección como `https://sano-movil-api.onrender.com`. Prueba `https://TU-DIRECCION/api/v1/salud`.
7. En Postman, cambia la variable `baseUrl` de la colección a `https://TU-DIRECCION/api/v1`.

## Parte 3 · Antes de la demostración
- **Despertar el servicio 5 a 10 minutos antes:** abre `/api/v1/salud` y espera. Si se durmió, la primera respuesta demora cerca de un minuto.
- Si quieres mantenerlo despierto durante la clase, un servicio gratuito como cron-job.org puede consultar `/salud` cada 10 minutos (desactívalo después).
- **Plan B:** ten el servicio corriendo en tu PC con el mismo `.env` (apuntando a Neon). Si falla la red de la sala, solo cambias `baseUrl` a `http://localhost:8080/api/v1`.
- En Render, mira la pestaña **Logs** durante la demo: sirve para mostrar el registro de peticiones.

## Seguridad al estar público
- Es internet de verdad: usa `JWT_SECRET` y `ADMIN_PASSWORD` largos y únicos.
- **Solo datos ficticios** en esta instancia (Anexo D del curso).
- HTTPS ya viene incluido con Render, lo que cumple el cifrado en tránsito.
- `TRUST_PROXY=true` solo en Render: así el límite de peticiones ve la IP real y no la del proxy.
- Activa en GitHub: *Settings > Code security > Dependabot alerts* y *Secret scanning*.
- Si una clave se llegara a subir a GitHub por error, cámbiala de inmediato (borrar el archivo no basta).
