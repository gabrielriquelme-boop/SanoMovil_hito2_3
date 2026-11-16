package cl.sanomovil.backend.service

import cl.sanomovil.backend.domain.Conflicto
import cl.sanomovil.backend.domain.Equipo
import cl.sanomovil.backend.domain.NoEncontrado
import cl.sanomovil.backend.domain.SolicitudInvalida
import cl.sanomovil.backend.domain.ValidacionFallida
import cl.sanomovil.backend.domain.Valores
import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.EquipoRepository
import cl.sanomovil.backend.repository.ModeloRepository
import java.sql.Connection

/** CAPA DE NEGOCIO: reglas de los equipos del usuario (máximo 8, textos válidos, modelo existente). */
class EquipoService(
    private val bd: BaseDeDatos,
    private val equipos: EquipoRepository,
    private val modelos: ModeloRepository,
) {
    fun listar(usuarioId: Int, tipo: String?): List<Equipo> {
        if (tipo != null && tipo !in Valores.TIPOS_EQUIPO) {
            throw SolicitudInvalida("El filtro 'tipo' debe ser telefono, tablet u otro")
        }
        return bd.transaccion { c -> equipos.listar(c, usuarioId, tipo) }
    }

    fun obtener(usuarioId: Int, id: Int): Equipo =
        bd.transaccion { c -> equipos.buscar(c, id, usuarioId) } ?: throw NoEncontrado("Equipo no encontrado")

    fun crear(usuarioId: Int, datos: DatosEquipo): Equipo {
        val d = validar(datos)
        return bd.transaccion { c ->
            if (equipos.contar(c, usuarioId) >= Valores.MAX_EQUIPOS_POR_USUARIO) {
                throw Conflicto("LIMITE_EQUIPOS", "Se alcanzó el máximo de ${Valores.MAX_EQUIPOS_POR_USUARIO} equipos")
            }
            exigirModelo(c, d.modeloId)
            val id = equipos.crear(c, usuarioId, d.modeloId, d.tipo, d.marca, d.modelo, d.apodo)
            equipos.buscar(c, id, usuarioId) ?: throw IllegalStateException("No se pudo leer el equipo recién creado")
        }
    }

    fun actualizar(usuarioId: Int, id: Int, datos: DatosEquipo): Equipo {
        val d = validar(datos)
        return bd.transaccion { c ->
            exigirModelo(c, d.modeloId)
            val filas = equipos.actualizar(c, id, usuarioId, d.modeloId, d.tipo, d.marca, d.modelo, d.apodo)
            if (filas == 0) throw NoEncontrado("Equipo no encontrado")
            equipos.buscar(c, id, usuarioId) ?: throw NoEncontrado("Equipo no encontrado")
        }
    }

    fun eliminar(usuarioId: Int, id: Int) {
        val filas = bd.transaccion { c -> equipos.eliminar(c, id, usuarioId) }
        if (filas == 0) throw NoEncontrado("Equipo no encontrado")
    }

    // ---- reglas internas ----
    private fun validar(datos: DatosEquipo): DatosEquipo {
        val marca = datos.marca.trim()
        val modelo = datos.modelo.trim()
        val apodo = datos.apodo?.trim()?.takeIf { it.isNotEmpty() }
        val errores = Errores()
        errores.exigir(datos.tipo in Valores.TIPOS_EQUIPO, "tipo: debe ser telefono, tablet u otro")
        errores.exigir(Validador.texto(marca, 40), "marca: 1 a 40 caracteres (letras, números, espacios, punto, guion)")
        errores.exigir(Validador.texto(modelo, 60), "modelo: 1 a 60 caracteres (letras, números, espacios, punto, guion)")
        if (apodo != null) errores.exigir(Validador.texto(apodo, 40), "apodo: 1 a 40 caracteres (letras, números, espacios, punto, guion)")
        errores.lanzarSiHay()
        return DatosEquipo(datos.tipo, marca, modelo, apodo, datos.modeloId)
    }

    private fun exigirModelo(c: Connection, modeloId: Int?) {
        if (modeloId != null && !modelos.existe(c, modeloId)) {
            throw ValidacionFallida(listOf("modelo_id: no existe en el catálogo de modelos"))
        }
    }
}
