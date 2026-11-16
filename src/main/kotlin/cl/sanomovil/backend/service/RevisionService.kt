package cl.sanomovil.backend.service

import cl.sanomovil.backend.domain.NoEncontrado
import cl.sanomovil.backend.domain.Revision
import cl.sanomovil.backend.domain.Valores
import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.EquipoRepository
import cl.sanomovil.backend.repository.RevisionRepository

/** CAPA DE NEGOCIO: historial de revisiones de un equipo (puntaje 0-100 y hallazgos). */
class RevisionService(
    private val bd: BaseDeDatos,
    private val equipos: EquipoRepository,
    private val revisiones: RevisionRepository,
) {
    fun listar(usuarioId: Int, equipoId: Int, limite: Int): List<Revision> =
        bd.transaccion { c ->
            equipos.buscar(c, equipoId, usuarioId) ?: throw NoEncontrado("Equipo no encontrado")
            revisiones.listarPorEquipo(c, equipoId, limite.coerceIn(1, 100))
        }

    fun obtener(usuarioId: Int, id: Int): Revision =
        bd.transaccion { c ->
            val revision = revisiones.buscarDeUsuario(c, id, usuarioId) ?: throw NoEncontrado("Revisión no encontrada")
            revision.copy(hallazgos = revisiones.hallazgosDe(c, id))
        }

    fun crear(usuarioId: Int, equipoId: Int, datos: DatosRevision): Revision {
        val nota = datos.nota?.trim()?.takeIf { it.isNotEmpty() }
        validar(datos, nota)
        return bd.transaccion { c ->
            equipos.buscar(c, equipoId, usuarioId) ?: throw NoEncontrado("Equipo no encontrado")
            val id = revisiones.crear(c, equipoId, datos.puntaje, datos.nivel, datos.bateriaPct, datos.espacioLibrePct, nota)
            datos.hallazgos.forEach { h ->
                revisiones.insertarHallazgo(c, id, h.codigo, h.nivel, h.titulo.trim(), h.detalle?.trim()?.takeIf { it.isNotEmpty() })
            }
            val revision = revisiones.buscarDeUsuario(c, id, usuarioId)
                ?: throw IllegalStateException("No se pudo leer la revisión recién creada")
            revision.copy(hallazgos = revisiones.hallazgosDe(c, id))
        }
    }

    fun actualizarNota(usuarioId: Int, id: Int, nota: String?): Revision {
        val limpia = nota?.trim()?.takeIf { it.isNotEmpty() }
        val errores = Errores()
        errores.exigir(limpia == null || limpia.length <= 200, "nota: máximo 200 caracteres")
        errores.lanzarSiHay()
        return bd.transaccion { c ->
            if (revisiones.actualizarNota(c, id, usuarioId, limpia) == 0) throw NoEncontrado("Revisión no encontrada")
            val revision = revisiones.buscarDeUsuario(c, id, usuarioId) ?: throw NoEncontrado("Revisión no encontrada")
            revision.copy(hallazgos = revisiones.hallazgosDe(c, id))
        }
    }

    fun eliminar(usuarioId: Int, id: Int) {
        val filas = bd.transaccion { c -> revisiones.eliminar(c, id, usuarioId) }
        if (filas == 0) throw NoEncontrado("Revisión no encontrada")
    }

    private fun validar(datos: DatosRevision, nota: String?) {
        val errores = Errores()
        errores.exigir(datos.puntaje in 0..100, "puntaje: debe estar entre 0 y 100")
        errores.exigir(datos.nivel in Valores.NIVELES, "nivel: debe ser ok, atencion o peligro")
        errores.exigir(datos.bateriaPct == null || datos.bateriaPct in 0..100, "bateria_pct: debe estar entre 0 y 100")
        errores.exigir(datos.espacioLibrePct == null || datos.espacioLibrePct in 0..100, "espacio_libre_pct: debe estar entre 0 y 100")
        errores.exigir(nota == null || nota.length <= 200, "nota: máximo 200 caracteres")
        errores.exigir(datos.hallazgos.size <= 30, "hallazgos: máximo 30 por revisión")
        datos.hallazgos.forEachIndexed { i, h ->
            errores.exigir(Validador.codigo(h.codigo), "hallazgos[$i].codigo: use minúsculas, números y guion bajo (máx. 40)")
            errores.exigir(h.nivel in Valores.NIVELES, "hallazgos[$i].nivel: debe ser ok, atencion o peligro")
            errores.exigir(h.titulo.trim().length in 1..120, "hallazgos[$i].titulo: 1 a 120 caracteres")
            errores.exigir((h.detalle?.trim()?.length ?: 0) <= 500, "hallazgos[$i].detalle: máximo 500 caracteres")
        }
        errores.lanzarSiHay()
    }
}
