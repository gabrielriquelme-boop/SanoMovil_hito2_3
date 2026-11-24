# Archivos de entrega: documento técnico PDF y aplicación APK

Esta guía explica cómo dejar los dos entregables descargables para la evaluación: el documento técnico en la carpeta `docs/` y la aplicación Android como recurso de una versión en GitHub Releases.

## 1. Subir el documento técnico PDF a docs/

1. Descarga el archivo `Documento_Tecnico_Hito2.pdf` que preparó el equipo.
2. Abre el repositorio: https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3
3. Entra a la carpeta **docs**.
4. Pulsa **Add file** y después **Upload files**.
5. Selecciona `Documento_Tecnico_Hito2.pdf` desde tu computador o arrástralo al área de carga.
6. Espera a que termine la carga. Comprueba que el nombre del archivo sea exactamente `Documento_Tecnico_Hito2.pdf`.
7. En **Commit changes**, escribe: `Agrega documento técnico PDF del Hito 2`.
8. Pulsa **Commit changes** para guardar el archivo en la rama principal.

El documento tiene 23 páginas, por debajo del máximo de 30 páginas indicado en la pauta.

## 2. Publicar SanoMovil.apk en GitHub Releases

La APK es el instalador Android. Se publica como recurso de una versión, separado de los archivos de código.

### Crear la versión

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

### Verificar la descarga

1. Abre la pestaña **Releases** del repositorio.
2. Entra a la versión que acabas de publicar.
3. En **Assets**, comprueba que aparezca `SanoMovil.apk`.
4. Haz clic en el archivo para comprobar que se descarga.

La página de versiones es:

https://github.com/gabrielriquelme-boop/SanoMovil_hito2_3/releases

## 3. Comprobación final de entregables

La pauta pide que el repositorio incluya:

- Código del servicio en la rama principal, con estructura de capas visible.
- Documento técnico PDF dentro de `docs/`, máximo 30 páginas sin contar anexos.
- Tabla o documentación de rutas, verbos, parámetros, cuerpo de ejemplo y códigos de estado.
- Modelo de datos y script de creación dentro de `db/`.
- Colección del cliente de pruebas lista para importar.
- Archivo de configuración de ejemplo en la raíz, sin credenciales reales.
- APK Android adjunta a una versión de GitHub Releases para que el profesor pueda descargarla.

## 4. Antes de la demostración

- Instala y prueba la misma APK que publicaste.
- Comprueba que el backend responda en `/api/v1/salud`.
- La instancia gratuita de Render puede tardar en responder si estuvo inactiva; abre la ruta de salud unos minutos antes de la demostración.
- No incluyas archivos `.env`, contraseñas, tokens ni credenciales en GitHub.
