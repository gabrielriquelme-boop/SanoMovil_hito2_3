package cl.sanomovil.backend.service

import cl.sanomovil.backend.domain.Conflicto
import cl.sanomovil.backend.domain.Modelo
import cl.sanomovil.backend.domain.NoEncontrado
import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.ModeloRepository

/** CAPA DE NEGOCIO: catálogo de modelos de equipos (lectura para todos, escritura solo administrador). */
class ModeloService(private val bd: BaseDeDatos, private val modelos: ModeloRepository) {
    fun listar(marca: String?): List<Modelo> =
        bd.transaccion { c -> modelos.listar(c, marca?.trim()?.takeIf { it.isNotEmpty() }) }

    fun obtener(id: Int): Modelo =
        bd.transaccion { c -> modelos.buscar(c, id) } ?: throw NoEncontrado("Modelo no encontrado")

    fun crear(datos: DatosModelo): Modelo {
        val d = validar(datos)
        return bd.transaccion { c ->
            if (modelos.existeMarcaNombre(c, d.marca, d.nombre, null)) {
                throw Conflicto("MODELO_DUPLICADO", "Ya existe un modelo con esa marca y nombre")
            }
            val id = modelos.crear(c, d.marca, d.nombre, d.anioLanzamiento, d.notas)
            modelos.buscar(c, id) ?: throw IllegalStateException("No se pudo leer el modelo recién creado")
        }
    }

    fun actualizar(id: Int, datos: DatosModelo): Modelo {
        val d = validar(datos)
        return bd.transaccion { c ->
            if (modelos.buscar(c, id) == null) throw NoEncontrado("Modelo no encontrado")
            if (modelos.existeMarcaNombre(c, d.marca, d.nombre, id)) {
                throw Conflicto("MODELO_DUPLICADO", "Ya existe un modelo con esa marca y nombre")
            }
            modelos.actualizar(c, id, d.marca, d.nombre, d.anioLanzamiento, d.notas)
            modelos.buscar(c, id) ?: throw NoEncontrado("Modelo no encontrado")
        }
    }

    fun eliminar(id: Int) {
        val filas = bd.transaccion { c -> modelos.eliminar(c, id) }
        if (filas == 0) throw NoEncontrado("Modelo no encontrado")
    }

    private fun validar(datos: DatosModelo): DatosModelo {
        val marca = datos.marca.trim()
        val nombre = datos.nombre.trim()
        val notas = datos.notas?.trim()?.takeIf { it.isNotEmpty() }
        val errores = Errores()
        errores.exigir(Validador.texto(marca, 40), "marca: 1 a 40 caracteres (letras, números, espacios, punto, guion)")
        errores.exigir(Validador.texto(nombre, 60), "nombre: 1 a 60 caracteres (letras, números, espacios, punto, guion)")
        errores.exigir(datos.anioLanzamiento == null || datos.anioLanzamiento in 2000..2100, "anio_lanzamiento: entre 2000 y 2100")
        errores.exigir(notas == null || notas.length <= 500, "notas: máximo 500 caracteres")
        errores.lanzarSiHay()
        return DatosModelo(marca, nombre, datos.anioLanzamiento, notas)
    }
}
