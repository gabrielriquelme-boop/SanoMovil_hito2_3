package cl.sanomovil.backend.service

/*
 * Datos de entrada de la capa de negocio. La capa de presentación convierte el JSON a estos
 * objetos; así los servicios no saben nada de HTTP ni de JSON.
 */
data class DatosRegistro(val correo: String, val nombre: String, val clave: String)
data class DatosLogin(val correo: String, val clave: String)
data class DatosEquipo(val tipo: String, val marca: String, val modelo: String, val apodo: String?, val modeloId: Int?)
data class DatosHallazgo(val codigo: String, val nivel: String, val titulo: String, val detalle: String?)
data class DatosRevision(
    val puntaje: Int,
    val nivel: String,
    val bateriaPct: Int?,
    val espacioLibrePct: Int?,
    val nota: String?,
    val hallazgos: List<DatosHallazgo>,
)
data class DatosModelo(val marca: String, val nombre: String, val anioLanzamiento: Int?, val notas: String?)
