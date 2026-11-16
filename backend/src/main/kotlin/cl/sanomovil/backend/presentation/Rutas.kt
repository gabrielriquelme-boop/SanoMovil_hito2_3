package cl.sanomovil.backend.presentation

import cl.sanomovil.backend.domain.SolicitudInvalida
import cl.sanomovil.backend.service.AuthService
import cl.sanomovil.backend.service.EquipoService
import cl.sanomovil.backend.service.ModeloService
import cl.sanomovil.backend.service.RevisionService
import cl.sanomovil.backend.service.SaludService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/*
 * CAPA DE PRESENTACIÓN (rutas): recibe la petición HTTP, valida el formato, llama al servicio
 * y arma la respuesta con el código de estado correcto. NO tiene reglas de negocio ni SQL.
 */

const val VERSION_API = "0.2.0"

fun Route.rutasSalud(servicio: SaludService) {
    get("/salud") {
        val bdOk = enHiloIO { servicio.baseDeDatosDisponible() }
        val cuerpo = SaludResp(
            estado = if (bdOk) "ok" else "degradado",
            baseDeDatos = if (bdOk) "ok" else "sin conexion",
            version = VERSION_API,
        )
        call.respond(if (bdOk) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable, cuerpo)
    }
}

// ---------------- Autenticación ----------------
fun Route.rutasAuthPublicas(auth: AuthService) {
    route("/auth") {
        post("/registro") {
            val req = call.recibirCuerpo<RegistroReq>()
            val usuario = enHiloIO { auth.registrar(req.aDatos()) }
            call.response.header(HttpHeaders.Location, "/api/v1/auth/yo")
            call.respond(HttpStatusCode.Created, usuario.aResp())
        }
        rateLimit(RateLimitName("login")) {
            post("/login") {
                val req = call.recibirCuerpo<LoginReq>()
                val sesion = enHiloIO { auth.iniciarSesion(req.aDatos()) }
                call.respond(HttpStatusCode.OK, sesion.aResp())
            }
        }
    }
}

fun Route.rutasAuthPrivadas(auth: AuthService) {
    route("/auth/yo") {
        get {
            val uid = call.usuarioId()
            call.respond(HttpStatusCode.OK, enHiloIO { auth.perfil(uid) }.aResp())
        }
        delete {
            val uid = call.usuarioId()
            enHiloIO { auth.eliminarCuenta(uid) }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ---------------- Equipos (y sus revisiones anidadas) ----------------
fun Route.rutasEquipos(equipos: EquipoService, revisiones: RevisionService) {
    route("/equipos") {
        get {
            val uid = call.usuarioId()
            val tipo = call.request.queryParameters["tipo"]
            val lista = enHiloIO { equipos.listar(uid, tipo) }.map { it.aResp() }
            call.respond(HttpStatusCode.OK, lista)
        }
        post {
            val uid = call.usuarioId()
            val req = call.recibirCuerpo<EquipoReq>()
            val creado = enHiloIO { equipos.crear(uid, req.aDatos()) }
            call.response.header(HttpHeaders.Location, "/api/v1/equipos/${creado.id}")
            call.respond(HttpStatusCode.Created, creado.aResp())
        }
        route("/{id}") {
            get {
                val uid = call.usuarioId()
                val id = call.idParam("id")
                call.respond(HttpStatusCode.OK, enHiloIO { equipos.obtener(uid, id) }.aResp())
            }
            put {
                val uid = call.usuarioId()
                val id = call.idParam("id")
                val req = call.recibirCuerpo<EquipoReq>()
                call.respond(HttpStatusCode.OK, enHiloIO { equipos.actualizar(uid, id, req.aDatos()) }.aResp())
            }
            delete {
                val uid = call.usuarioId()
                val id = call.idParam("id")
                enHiloIO { equipos.eliminar(uid, id) }
                call.respond(HttpStatusCode.NoContent)
            }
            route("/revisiones") {
                get {
                    val uid = call.usuarioId()
                    val equipoId = call.idParam("id")
                    val textoLimite = call.request.queryParameters["limite"]
                    val limite = if (textoLimite == null) 20 else
                        textoLimite.toIntOrNull() ?: throw SolicitudInvalida("El parámetro 'limite' debe ser un entero")
                    val lista = enHiloIO { revisiones.listar(uid, equipoId, limite) }.map { it.aResumen() }
                    call.respond(HttpStatusCode.OK, lista)
                }
                post {
                    val uid = call.usuarioId()
                    val equipoId = call.idParam("id")
                    val req = call.recibirCuerpo<RevisionReq>()
                    val creada = enHiloIO { revisiones.crear(uid, equipoId, req.aDatos()) }
                    call.response.header(HttpHeaders.Location, "/api/v1/revisiones/${creada.id}")
                    call.respond(HttpStatusCode.Created, creada.aResp())
                }
            }
        }
    }
}

// ---------------- Revisiones ----------------
fun Route.rutasRevisiones(revisiones: RevisionService) {
    route("/revisiones/{id}") {
        get {
            val uid = call.usuarioId()
            val id = call.idParam("id")
            call.respond(HttpStatusCode.OK, enHiloIO { revisiones.obtener(uid, id) }.aResp())
        }
        patch {
            val uid = call.usuarioId()
            val id = call.idParam("id")
            val req = call.recibirCuerpo<NotaReq>()
            call.respond(HttpStatusCode.OK, enHiloIO { revisiones.actualizarNota(uid, id, req.nota) }.aResp())
        }
        delete {
            val uid = call.usuarioId()
            val id = call.idParam("id")
            enHiloIO { revisiones.eliminar(uid, id) }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ---------------- Catálogo de modelos ----------------
fun Route.rutasModelos(modelos: ModeloService) {
    route("/modelos") {
        get {
            val marca = call.request.queryParameters["marca"]
            call.respond(HttpStatusCode.OK, enHiloIO { modelos.listar(marca) }.map { it.aResp() })
        }
        post {
            call.exigirAdmin()
            val req = call.recibirCuerpo<ModeloReq>()
            val creado = enHiloIO { modelos.crear(req.aDatos()) }
            call.response.header(HttpHeaders.Location, "/api/v1/modelos/${creado.id}")
            call.respond(HttpStatusCode.Created, creado.aResp())
        }
        route("/{id}") {
            get {
                val id = call.idParam("id")
                call.respond(HttpStatusCode.OK, enHiloIO { modelos.obtener(id) }.aResp())
            }
            put {
                call.exigirAdmin()
                val id = call.idParam("id")
                val req = call.recibirCuerpo<ModeloReq>()
                call.respond(HttpStatusCode.OK, enHiloIO { modelos.actualizar(id, req.aDatos()) }.aResp())
            }
            delete {
                call.exigirAdmin()
                val id = call.idParam("id")
                enHiloIO { modelos.eliminar(id) }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
