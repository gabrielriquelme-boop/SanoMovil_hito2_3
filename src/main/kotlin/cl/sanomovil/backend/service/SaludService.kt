package cl.sanomovil.backend.service

import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.consultar

/** Comprueba que el servicio y la base de datos responden (endpoint /salud). */
class SaludService(private val bd: BaseDeDatos) {
    fun baseDeDatosDisponible(): Boolean =
        try {
            bd.transaccion { c -> c.consultar("SELECT 1 AS uno") { it.getInt("uno") } }.isNotEmpty()
        } catch (e: Exception) {
            false
        }
}
