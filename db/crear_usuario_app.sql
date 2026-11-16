-- =====================================================================
-- Usuario de la APLICACIÓN con permisos mínimos (principio de menor privilegio).
-- El servicio NO debe conectarse como "postgres" (superusuario): si alguien lograra
-- inyectar SQL, con este usuario no podría borrar tablas, crear usuarios ni leer otras bases.
--
-- Ejecutar DESPUÉS de crear la base, y pasando la clave por variable (NO escribirla en este archivo):
--     psql -U postgres -d sanomovil -v clave="'ElIgeUnaClaveLargaYUnica'" -f db/crear_usuario_app.sql
-- Con pgAdmin: abre el Query Tool sobre la base sanomovil, pega este contenido y reemplaza
-- :clave por tu clave entre comillas simples (por ejemplo 'MiClave123'). No guardes ese cambio en el archivo.
-- Luego, en el .env:  DB_USER=sano_app   y   DB_PASSWORD=<esa clave>
--
-- OJO: si vuelves a ejecutar db/schema.sql (borra y recrea las tablas), hay que volver a correr este script.
-- =====================================================================
CREATE ROLE sano_app LOGIN PASSWORD :clave NOSUPERUSER NOCREATEDB NOCREATEROLE;

REVOKE ALL ON DATABASE sanomovil FROM PUBLIC;
GRANT CONNECT ON DATABASE sanomovil TO sano_app;
GRANT USAGE ON SCHEMA public TO sano_app;

-- Solo puede leer y escribir datos. No puede crear ni borrar tablas.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sano_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sano_app;
