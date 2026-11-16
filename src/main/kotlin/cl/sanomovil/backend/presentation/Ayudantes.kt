package cl.sanomovil.backend.presentation

import cl.sanomovil.backend.domain.AppException
import cl.sanomovil.backend.domain.NoAutenticado
import cl.sanomovil.backend.domain.Prohibido
import cl.sanomovil.backend.domain.Rol
import cl.sanomovil.backend.domain.SolicitudInvalida
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Las consultas a la base son bloqueantes: se ejecutan en el hilo de IO para no frenar el servidor. */
suspend fun <T> enHiloIO(bloque: () -> T): T = withContext(Dispatchers.IO) { bloque() }

/** Id del usuario que viene dentro del token JWT (campo "sub"). */
fun ApplicationCall.usuarioId(): Int =
    principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() ?: throw NoAutenticado("Token inválido")

fun ApplicationCall.exigirAdmin() {
    val rol = principal<JWTPrincipal>()?.payload?.getClaim("rol")?.asString()
    if (rol != Rol.ADMIN) throw Prohibido("Se requiere rol de administrador")
}

/** Lee un parámetro de la ruta (por ejemplo {id}) como entero positivo, o responde 400. */
fun ApplicationCall.idParam(nombre: String): Int =
    parameters[nombre]?.toIntOrNull()?.takeIf { it > 0 }
        ?: throw SolicitudInvalida("El parámetro '$nombre' debe ser un entero positivo")

/** Máximo de bytes que aceptamos en un cuerpo JSON (evita que alguien nos envíe archivos gigantes). */
const val MAX_CUERPO_BYTES = 100_000L

/** Lee el cuerpo JSON; si viene mal formado o le faltan campos obligatorios, responde 400. */
suspend inline fun <reified T : Any> ApplicationCall.recibirCuerpo(): T {
    val largo = request.contentLength()
    if (largo != null && largo > MAX_CUERPO_BYTES) {
        throw AppException(413, "CUERPO_DEMASIADO_GRANDE", "El cuerpo de la petición supera el máximo de 100 KB")
    }
    return try {
        receive<T>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw SolicitudInvalida("El cuerpo de la petición no es un JSON válido o le faltan campos obligatorios")
    }
}
