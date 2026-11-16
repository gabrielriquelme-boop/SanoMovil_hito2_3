-- =====================================================================
-- Sano Móvil · Modelo de datos (PostgreSQL 14 o superior)
-- Crea todas las tablas desde cero. Se puede ejecutar más de una vez.
-- =====================================================================

DROP TABLE IF EXISTS registro_accesos CASCADE;
DROP TABLE IF EXISTS hallazgos        CASCADE;
DROP TABLE IF EXISTS revisiones       CASCADE;
DROP TABLE IF EXISTS equipos          CASCADE;
DROP TABLE IF EXISTS modelos          CASCADE;
DROP TABLE IF EXISTS usuarios         CASCADE;

-- Personas que usan la app. La clave se guarda SOLO como hash BCrypt.
CREATE TABLE usuarios (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo      varchar(254) NOT NULL UNIQUE,
    nombre      varchar(60)  NOT NULL,
    hash_clave  varchar(100) NOT NULL,
    rol         varchar(10)  NOT NULL DEFAULT 'usuario' CHECK (rol IN ('usuario', 'admin')),
    creado_en   timestamptz  NOT NULL DEFAULT now()
);

-- Catálogo de modelos conocidos (lo administra el rol admin).
CREATE TABLE modelos (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    marca            varchar(40) NOT NULL,
    nombre           varchar(60) NOT NULL,
    anio_lanzamiento smallint    CHECK (anio_lanzamiento BETWEEN 2000 AND 2100),
    notas            varchar(500),
    UNIQUE (marca, nombre)
);

-- Equipos que cada usuario agrega ("Mis equipos"). Si se borra el usuario, se borran sus equipos.
CREATE TABLE equipos (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  integer     NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    modelo_id   integer     REFERENCES modelos (id) ON DELETE SET NULL,
    tipo        varchar(10) NOT NULL CHECK (tipo IN ('telefono', 'tablet', 'otro')),
    marca       varchar(40) NOT NULL,
    modelo      varchar(60) NOT NULL,
    apodo       varchar(40),
    creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_equipos_usuario ON equipos (usuario_id);

-- Cada vez que se revisa un equipo se guarda una revisión (puntaje de salud 0-100).
CREATE TABLE revisiones (
    id                 integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipo_id          integer     NOT NULL REFERENCES equipos (id) ON DELETE CASCADE,
    puntaje            smallint    NOT NULL CHECK (puntaje BETWEEN 0 AND 100),
    nivel              varchar(10) NOT NULL CHECK (nivel IN ('ok', 'atencion', 'peligro')),
    bateria_pct        smallint    CHECK (bateria_pct BETWEEN 0 AND 100),
    espacio_libre_pct  smallint    CHECK (espacio_libre_pct BETWEEN 0 AND 100),
    nota               varchar(200),
    creado_en          timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_revisiones_equipo ON revisiones (equipo_id, creado_en DESC);

-- Lo que encontró cada revisión (por ejemplo "actualización de seguridad atrasada").
CREATE TABLE hallazgos (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revision_id  integer      NOT NULL REFERENCES revisiones (id) ON DELETE CASCADE,
    codigo       varchar(40)  NOT NULL,
    nivel        varchar(10)  NOT NULL CHECK (nivel IN ('ok', 'atencion', 'peligro')),
    titulo       varchar(120) NOT NULL,
    detalle      varchar(500)
);
CREATE INDEX idx_hallazgos_revision ON hallazgos (revision_id);

-- Registro de accesos (seguridad y Ley 21.719). Guarda solo quién (id), qué y cuándo; no el correo.
CREATE TABLE registro_accesos (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  integer     REFERENCES usuarios (id) ON DELETE SET NULL,
    accion      varchar(20) NOT NULL CHECK (accion IN ('registro', 'login_ok', 'login_fallido', 'eliminar_cuenta')),
    creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_accesos_usuario ON registro_accesos (usuario_id, creado_en DESC);
