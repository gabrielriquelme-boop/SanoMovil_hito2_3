# Publicar SanoMovil.apk en GitHub Releases

Esta guía explica cómo adjuntar el instalador Android al repositorio sin incorporarlo al historial del código fuente.

## Antes de empezar

Ten disponible el archivo `SanoMovil.apk` (la versión de demostración entregada por el equipo). Comprueba que el nombre sea exactamente `SanoMovil.apk`.

## Crear la versión

1. Abre el repositorio: https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3
2. En la parte superior, entra en **Releases**.
3. Selecciona **Create a new release** o **Draft a new release**.
4. En **Choose a tag**, escribe `v1.0.0` y elige la opción para crear una etiqueta nueva.
5. Como título escribe `SanoMovil v1.0.0 - APK Android`.
6. En la descripción puedes poner:

   ```text
   Versión de demostración de SanoMovil para Android.
   Esta APK se conecta al servicio REST desplegado por el equipo.
   ```

7. En el área **Attach binaries by dropping them here or selecting them**, selecciona el archivo `SanoMovil.apk` o arrástralo a esa zona.
8. Espera a que termine la carga y comprueba que el archivo aparezca adjunto con el nombre correcto.
9. Pulsa **Publish release**.

## Verificar la descarga

1. Abre la pestaña **Releases** del repositorio.
2. Entra a la versión que acabas de publicar.
3. En **Assets**, comprueba que aparezca `SanoMovil.apk`.
4. Haz clic en el archivo para comprobar que se descarga.

La página de versiones es:

https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases

## Por qué usamos Releases

La APK es un archivo binario de instalación, no código fuente. Publicarla como recurso de una versión mantiene la carpeta del código más ordenada y permite identificar qué versión de la app se entregó.

## Antes de la demostración

- Instala y prueba la misma APK que publicaste.
- Comprueba que el backend responda en `/api/v1/salud`.
- La instancia gratuita de Render puede tardar en responder si estuvo inactiva; abre la ruta de salud unos minutos antes de la demostración.
- No incluyas archivos `.env`, contraseñas, tokens ni credenciales en la versión.
