package cl.sanomovil.backend.domain

import java.time.Instant

// ---------- Constantes del dominio ----------
object Rol {
    const val USUARIO = "usuario"
    const val ADMIN = "admin"
}

object Valores {
    val NIVELES = setOf("ok", "atencion", "peligro")
    val TIPOS_EQUIPO = setOf("telefono", "tablet", "otro")
    const val MAX_EQUIPOS_POR_USUARIO = 8
    const val MAX_INTENTOS_LOGIN = 5          // intentos fallidos permitidos...
    const val VENTANA_BLOQUEO_MIN = 15        // ...dentro de esta ventana de minutos
}

object Accion {
    const val REGISTRO = "registro"
    const val LOGIN_OK = "login_ok"
    const val LOGIN_FALLIDO = "login_fallido"
    const val ELIMINAR_CUENTA = "eliminar_cuenta"
}

// ---------- Entidades ----------
data class Usuario(
    val id: Int,
    val correo: String,
    val nombre: String,
    val hashClave: String,
    val rol: String,
    val creadoEn: Instant,
)

data class Modelo(
    val id: Int,
    val marca: String,
    val nombre: String,
    val anioLanzamiento: Int?,
    val notas: String?,
)

data class Equipo(
    val id: Int,
    val usuarioId: Int,
    val modeloId: Int?,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val apodo: String?,
    val creadoEn: Instant,
)

data class Hallazgo(
    val id: Int,
    val revisionId: Int,
    val codigo: String,
    val nivel: String,
    val titulo: String,
    val detalle: String?,
)

data class Revision(
    val id: Int,
    val equipoId: Int,
    val puntaje: Int,
    val nivel: String,
    val bateriaPct: Int?,
    val espacioLibrePct: Int?,
    val nota: String?,
    val creadoEn: Instant,
    val hallazgos: List<Hallazgo> = emptyList(),
)

data class Sesion(val usuario: Usuario, val token: String, val expiraEnSeg: Long)

// ---------- Errores del dominio (el estado HTTP se decide aquí, el formato en la capa de presentación) ----------
open class AppException(
    val status: Int,
    val codigo: String,
    mensaje: String,
    val detalles: List<String> = emptyList(),
) : RuntimeException(mensaje)

class SolicitudInvalida(mensaje: String) : AppException(400, "SOLICITUD_INVALIDA", mensaje)
class NoAutenticado(mensaje: String) : AppException(401, "NO_AUTENTICADO", mensaje)
class Prohibido(mensaje: String) : AppException(403, "PROHIBIDO", mensaje)
class NoEncontrado(mensaje: String) : AppException(404, "NO_ENCONTRADO", mensaje)
class Conflicto(codigo: String, mensaje: String) : AppException(409, codigo, mensaje)
class DemasiadosIntentos(mensaje: String) : AppException(429, "DEMASIADOS_INTENTOS", mensaje)
class ValidacionFallida(detalles: List<String>) :
    AppException(422, "VALIDACION_FALLIDA", "Los datos enviados no son válidos", detalles)
