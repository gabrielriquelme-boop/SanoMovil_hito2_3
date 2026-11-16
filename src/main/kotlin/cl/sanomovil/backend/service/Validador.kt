package cl.sanomovil.backend.service

import cl.sanomovil.backend.domain.ValidacionFallida

/** Reglas de validación reutilizables. Devuelven true/false; el servicio decide qué error lanzar. */
object Validador {
    private val CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val TEXTO = Regex("^[\\p{L}\\p{N} ._-]+$")
    private val CODIGO = Regex("^[a-z0-9_]{1,40}$")

    fun correo(valor: String): Boolean = valor.length <= 254 && CORREO.matches(valor)

    /** BCrypt solo usa los primeros 72 caracteres, por eso ese es el máximo. */
    fun clave(valor: String): Boolean =
        valor.length in 8..72 && valor.any { it.isLetter() } && valor.any { it.isDigit() }

    fun texto(valor: String, maximo: Int): Boolean = valor.length in 1..maximo && TEXTO.matches(valor)

    fun codigo(valor: String): Boolean = CODIGO.matches(valor)
}

/** Junta todos los errores de validación para devolverlos juntos (422), no de a uno. */
class Errores {
    private val lista = mutableListOf<String>()

    fun exigir(condicion: Boolean, mensaje: String) {
        if (!condicion) lista.add(mensaje)
    }

    fun lanzarSiHay() {
        if (lista.isNotEmpty()) throw ValidacionFallida(lista.toList())
    }
}
