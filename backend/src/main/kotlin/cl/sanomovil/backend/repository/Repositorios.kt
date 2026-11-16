package cl.sanomovil.backend.repository

import cl.sanomovil.backend.domain.Equipo
import cl.sanomovil.backend.domain.Hallazgo
import cl.sanomovil.backend.domain.Modelo
import cl.sanomovil.backend.domain.Revision
import cl.sanomovil.backend.domain.Usuario
import java.sql.Connection
import java.sql.ResultSet

/*
 * CAPA DE DATOS: aquí vive TODO el SQL. Ninguna otra capa escribe consultas.
 * Los repositorios no validan reglas de negocio: solo leen y escriben.
 */

class UsuarioRepository {
    private val columnas = "id, correo, nombre, hash_clave, rol, creado_en"

    fun buscarPorCorreo(c: Connection, correo: String): Usuario? =
        c.consultar("SELECT $columnas FROM usuarios WHERE correo = ?", correo) { aUsuario(it) }.firstOrNull()

    fun buscarPorId(c: Connection, id: Int): Usuario? =
        c.consultar("SELECT $columnas FROM usuarios WHERE id = ?", id) { aUsuario(it) }.firstOrNull()

    fun crear(c: Connection, correo: String, nombre: String, hashClave: String, rol: String): Int =
        c.consultar(
            "INSERT INTO usuarios (correo, nombre, hash_clave, rol) VALUES (?, ?, ?, ?) RETURNING id",
            correo, nombre, hashClave, rol,
        ) { it.getInt("id") }.first()

    fun eliminar(c: Connection, id: Int): Int = c.ejecutar("DELETE FROM usuarios WHERE id = ?", id)

    private fun aUsuario(rs: ResultSet) = Usuario(
        id = rs.getInt("id"),
        correo = rs.getString("correo"),
        nombre = rs.getString("nombre"),
        hashClave = rs.getString("hash_clave"),
        rol = rs.getString("rol"),
        creadoEn = rs.getTimestamp("creado_en").toInstant(),
    )
}

class ModeloRepository {
    private val columnas = "id, marca, nombre, anio_lanzamiento, notas"

    fun listar(c: Connection, marca: String?): List<Modelo> =
        if (marca == null) {
            c.consultar("SELECT $columnas FROM modelos ORDER BY marca, nombre") { aModelo(it) }
        } else {
            c.consultar("SELECT $columnas FROM modelos WHERE marca ILIKE ? ORDER BY marca, nombre", marca) { aModelo(it) }
        }

    fun buscar(c: Connection, id: Int): Modelo? =
        c.consultar("SELECT $columnas FROM modelos WHERE id = ?", id) { aModelo(it) }.firstOrNull()

    fun existe(c: Connection, id: Int): Boolean =
        c.consultar("SELECT 1 AS uno FROM modelos WHERE id = ?", id) { it.getInt("uno") }.isNotEmpty()

    fun existeMarcaNombre(c: Connection, marca: String, nombre: String, exceptoId: Int?): Boolean =
        c.consultar(
            "SELECT id FROM modelos WHERE lower(marca) = lower(?) AND lower(nombre) = lower(?) AND id <> ?",
            marca, nombre, exceptoId ?: -1,
        ) { it.getInt("id") }.isNotEmpty()

    fun crear(c: Connection, marca: String, nombre: String, anio: Int?, notas: String?): Int =
        c.consultar(
            "INSERT INTO modelos (marca, nombre, anio_lanzamiento, notas) VALUES (?, ?, ?, ?) RETURNING id",
            marca, nombre, anio, notas,
        ) { it.getInt("id") }.first()

    fun actualizar(c: Connection, id: Int, marca: String, nombre: String, anio: Int?, notas: String?): Int =
        c.ejecutar(
            "UPDATE modelos SET marca = ?, nombre = ?, anio_lanzamiento = ?, notas = ? WHERE id = ?",
            marca, nombre, anio, notas, id,
        )

    fun eliminar(c: Connection, id: Int): Int = c.ejecutar("DELETE FROM modelos WHERE id = ?", id)

    private fun aModelo(rs: ResultSet) = Modelo(
        id = rs.getInt("id"),
        marca = rs.getString("marca"),
        nombre = rs.getString("nombre"),
        anioLanzamiento = rs.enteroONulo("anio_lanzamiento"),
        notas = rs.getString("notas"),
    )
}

class EquipoRepository {
    private val columnas = "id, usuario_id, modelo_id, tipo, marca, modelo, apodo, creado_en"

    fun listar(c: Connection, usuarioId: Int, tipo: String?): List<Equipo> =
        if (tipo == null) {
            c.consultar("SELECT $columnas FROM equipos WHERE usuario_id = ? ORDER BY id", usuarioId) { aEquipo(it) }
        } else {
            c.consultar(
                "SELECT $columnas FROM equipos WHERE usuario_id = ? AND tipo = ? ORDER BY id",
                usuarioId, tipo,
            ) { aEquipo(it) }
        }

    fun contar(c: Connection, usuarioId: Int): Int =
        c.consultar("SELECT count(*) AS total FROM equipos WHERE usuario_id = ?", usuarioId) { it.getInt("total") }.first()

    /** Busca un equipo SOLO si pertenece al usuario (así nadie ve equipos ajenos). */
    fun buscar(c: Connection, id: Int, usuarioId: Int): Equipo? =
        c.consultar("SELECT $columnas FROM equipos WHERE id = ? AND usuario_id = ?", id, usuarioId) { aEquipo(it) }
            .firstOrNull()

    fun crear(
        c: Connection, usuarioId: Int, modeloId: Int?, tipo: String, marca: String, modelo: String, apodo: String?,
    ): Int = c.consultar(
        "INSERT INTO equipos (usuario_id, modelo_id, tipo, marca, modelo, apodo) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
        usuarioId, modeloId, tipo, marca, modelo, apodo,
    ) { it.getInt("id") }.first()

    fun actualizar(
        c: Connection, id: Int, usuarioId: Int, modeloId: Int?, tipo: String, marca: String, modelo: String, apodo: String?,
    ): Int = c.ejecutar(
        "UPDATE equipos SET modelo_id = ?, tipo = ?, marca = ?, modelo = ?, apodo = ? WHERE id = ? AND usuario_id = ?",
        modeloId, tipo, marca, modelo, apodo, id, usuarioId,
    )

    fun eliminar(c: Connection, id: Int, usuarioId: Int): Int =
        c.ejecutar("DELETE FROM equipos WHERE id = ? AND usuario_id = ?", id, usuarioId)

    private fun aEquipo(rs: ResultSet) = Equipo(
        id = rs.getInt("id"),
        usuarioId = rs.getInt("usuario_id"),
        modeloId = rs.enteroONulo("modelo_id"),
        tipo = rs.getString("tipo"),
        marca = rs.getString("marca"),
        modelo = rs.getString("modelo"),
        apodo = rs.getString("apodo"),
        creadoEn = rs.getTimestamp("creado_en").toInstant(),
    )
}

class RevisionRepository {
    private val columnas = "r.id, r.equipo_id, r.puntaje, r.nivel, r.bateria_pct, r.espacio_libre_pct, r.nota, r.creado_en"

    fun listarPorEquipo(c: Connection, equipoId: Int, limite: Int): List<Revision> =
        c.consultar(
            "SELECT $columnas FROM revisiones r WHERE r.equipo_id = ? ORDER BY r.creado_en DESC, r.id DESC LIMIT ?",
            equipoId, limite,
        ) { aRevision(it) }

    /** Busca una revisión SOLO si su equipo pertenece al usuario. */
    fun buscarDeUsuario(c: Connection, id: Int, usuarioId: Int): Revision? =
        c.consultar(
            "SELECT $columnas FROM revisiones r JOIN equipos e ON e.id = r.equipo_id WHERE r.id = ? AND e.usuario_id = ?",
            id, usuarioId,
        ) { aRevision(it) }.firstOrNull()

    fun crear(
        c: Connection, equipoId: Int, puntaje: Int, nivel: String, bateriaPct: Int?, espacioLibrePct: Int?, nota: String?,
    ): Int = c.consultar(
        "INSERT INTO revisiones (equipo_id, puntaje, nivel, bateria_pct, espacio_libre_pct, nota) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
        equipoId, puntaje, nivel, bateriaPct, espacioLibrePct, nota,
    ) { it.getInt("id") }.first()

    fun insertarHallazgo(c: Connection, revisionId: Int, codigo: String, nivel: String, titulo: String, detalle: String?) {
        c.ejecutar(
            "INSERT INTO hallazgos (revision_id, codigo, nivel, titulo, detalle) VALUES (?, ?, ?, ?, ?)",
            revisionId, codigo, nivel, titulo, detalle,
        )
    }

    fun hallazgosDe(c: Connection, revisionId: Int): List<Hallazgo> =
        c.consultar(
            "SELECT id, revision_id, codigo, nivel, titulo, detalle FROM hallazgos WHERE revision_id = ? ORDER BY id",
            revisionId,
        ) {
            Hallazgo(
                id = it.getInt("id"),
                revisionId = it.getInt("revision_id"),
                codigo = it.getString("codigo"),
                nivel = it.getString("nivel"),
                titulo = it.getString("titulo"),
                detalle = it.getString("detalle"),
            )
        }

    fun actualizarNota(c: Connection, id: Int, usuarioId: Int, nota: String?): Int =
        c.ejecutar(
            "UPDATE revisiones SET nota = ? FROM equipos " +
                "WHERE revisiones.id = ? AND equipos.id = revisiones.equipo_id AND equipos.usuario_id = ?",
            nota, id, usuarioId,
        )

    fun eliminar(c: Connection, id: Int, usuarioId: Int): Int =
        c.ejecutar(
            "DELETE FROM revisiones WHERE id = ? AND equipo_id IN (SELECT id FROM equipos WHERE usuario_id = ?)",
            id, usuarioId,
        )

    private fun aRevision(rs: ResultSet) = Revision(
        id = rs.getInt("id"),
        equipoId = rs.getInt("equipo_id"),
        puntaje = rs.getInt("puntaje"),
        nivel = rs.getString("nivel"),
        bateriaPct = rs.enteroONulo("bateria_pct"),
        espacioLibrePct = rs.enteroONulo("espacio_libre_pct"),
        nota = rs.getString("nota"),
        creadoEn = rs.getTimestamp("creado_en").toInstant(),
    )
}

/** Registro de accesos (autenticación): sirve de evidencia de seguridad y cumple la Ley 21.719. */
class AccesoRepository {
    fun registrar(c: Connection, usuarioId: Int?, accion: String) {
        c.ejecutar("INSERT INTO registro_accesos (usuario_id, accion) VALUES (?, ?)", usuarioId, accion)
    }

    /** Cuenta los inicios de sesión fallidos de un usuario en los últimos N minutos (defensa contra fuerza bruta). */
    fun contarFallidosRecientes(c: Connection, usuarioId: Int, minutos: Int): Int =
        c.consultar(
            "SELECT count(*) AS total FROM registro_accesos " +
                "WHERE usuario_id = ? AND accion = 'login_fallido' AND creado_en > now() - (? * interval '1 minute')",
            usuarioId, minutos,
        ) { it.getInt("total") }.first()
}
