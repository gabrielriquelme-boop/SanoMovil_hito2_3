# Guía de instalación en Windows (sin Docker)

Tiempo estimado: 20 a 30 minutos. Se hace una sola vez.

## 1. Instalar PostgreSQL
1. Descargar el instalador de Windows desde la página oficial: https://www.postgresql.org/download/windows/ (enlace "Download the installer", de EDB). Elegir la versión estable más reciente (16 o superior).
2. Ejecutarlo y dejar marcados **PostgreSQL Server**, **pgAdmin 4** y **Command Line Tools**.
3. Cuando pida la contraseña del superusuario `postgres`, **escribe una y anótala**: la necesitarás en el `.env`.
4. Dejar el puerto en **5432** y el resto por defecto. Al final no hace falta lanzar Stack Builder.

## 2. Crear la base de datos
**Opción A: con pgAdmin (la más fácil)**
1. Abrir pgAdmin e ingresar la contraseña del paso anterior.
2. Clic derecho en *Databases* > *Create* > *Database…*, nombre `sanomovil`, y *Save*.
3. Clic derecho sobre `sanomovil` > *Query Tool*. Abrir el archivo `db/schema.sql` (ícono de carpeta), ejecutar con el botón ▶, y repetir con `db/seed.sql`.
4. Para comprobar: *sanomovil > Schemas > public > Tables* debe mostrar 6 tablas.

**Opción B: con la terminal**
Abrir PowerShell en la raíz del repositorio y ejecutar (ajusta el número de versión de tu carpeta):
```
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -f db/crear_base.sql
```

## 3. Configurar el `.env`
1. En la raíz del repositorio, copiar `.env.example` y renombrar la copia a `.env`.
2. Editar `DB_PASSWORD` (la clave de PostgreSQL) y `JWT_SECRET` (cualquier texto largo y aleatorio de 32 o más caracteres).
3. **Nunca subir el `.env` a GitHub.** Ya está en el `.gitignore`.

## 3.1 (Recomendado) Usuario de la base con permisos mínimos
El servicio no debería conectarse como `postgres` (superusuario). Para crear el usuario `sano_app`:
1. En pgAdmin, clic derecho sobre `sanomovil` > *Query Tool*.
2. Pegar el contenido de `db/crear_usuario_app.sql`, reemplazar `:clave` por una clave entre comillas simples (por ejemplo `'MiClaveLarga2026'`) y ejecutar ▶. **No guardes ese cambio en el archivo.**
3. En el `.env`, poner `DB_USER=sano_app` y `DB_PASSWORD=` con esa clave.
Si vuelves a ejecutar `db/schema.sql`, hay que repetir este paso.

## 4. Arrancar el servicio
1. Abrir Android Studio. *File > Open* y elegir la carpeta **`backend`** del repositorio.
2. Esperar a que termine la sincronización de Gradle (la primera vez descarga librerías y puede tardar varios minutos).
3. Abrir la ventana *Gradle* (panel derecho), ir a *Tasks > application > run* y hacer doble clic.
4. En la consola debe aparecer una línea con `Responding at http://0.0.0.0:8080`.
5. Abrir http://localhost:8080/api/v1/salud en el navegador. Debe mostrar `"estado": "ok"`.

**Si la terminal dice que no encuentra Java:** definir `JAVA_HOME` apuntando al JDK de Android Studio, por ejemplo
`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` y volver a ejecutar `.\gradlew run`.

**Errores frecuentes**
- *"Falta la variable DB_PASSWORD"*: el `.env` no está en la raíz del repositorio o no se llama exactamente `.env`.
- *"password authentication failed"*: la clave en `DB_PASSWORD` no es la de PostgreSQL.
- *"Connection refused" o "database sanomovil does not exist"*: PostgreSQL no está corriendo, o falta el paso 2.
- *"Address already in use"*: otro programa usa el puerto 8080; cambia `PORT` en el `.env`.

## 5. Probar con Postman
1. Instalar Postman (https://www.postman.com/downloads/) o Bruno (https://www.usebruno.com/), que no exige crear cuenta.
2. *Import* y elegir `docs/sano-movil.postman_collection.json`.
3. Con el servicio corriendo, ejecutar las carpetas **en orden, de la 0 a la 7**. Cada petición tiene pruebas automáticas que muestran si el código de estado es el esperado.
