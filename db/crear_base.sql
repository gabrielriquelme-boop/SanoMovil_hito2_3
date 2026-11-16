-- =====================================================================
-- Crea la base "sanomovil" desde cero, con sus tablas y datos de ejemplo.
-- Ejecutar DESDE LA RAÍZ del repositorio:
--     psql -U postgres -f db/crear_base.sql
-- (Si usas pgAdmin en vez de psql, mira la sección "Base de datos" del README.)
-- =====================================================================
DROP DATABASE IF EXISTS sanomovil;
CREATE DATABASE sanomovil;
\connect sanomovil
\i db/schema.sql
\i db/seed.sql
