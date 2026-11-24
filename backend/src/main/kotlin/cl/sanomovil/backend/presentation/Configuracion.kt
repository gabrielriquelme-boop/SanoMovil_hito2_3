package cl.sanomovil.backend.presentation

import cl.sanomovil.backend.config.Config
import cl.sanomovil.backend.domain.AppException
import cl.sanomovil.backend.Servicios
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheader.XForwardedHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

/** Arma el servidor: formato JSON, manejo de errores, autenticación JWT y el mapa de rutas. */
fun Application.configurar(config: Config, servicios: Servicios) {
    install(CallLogging)

    // Detrás de un proxy (Render, Nginx...) la IP real del cliente viene en la cabecera X-Forwarded-For.
    // SOLO se activa con TRUST_PROXY=true: si no hay proxy, cualquiera podría falsear su IP y saltarse el límite.
    if (config.confiarProxy) {
        install(XForwardedHeaders)
    }

    // Cabeceras de seguridad en TODAS las respuestas
    install(DefaultHeaders) {
        header("Server", "sano-movil")                 // no revelamos la tecnología ni su versión
        header("X-Content-Type-Options", "nosniff")    // el navegador no "adivina" tipos de archivo
        header("X-Frame-Options", "DENY")              // nadie puede incrustar la API en un marco
        header("Referrer-Policy", "no-referrer")
        header("Cache-Control", "no-store")            // las respuestas con datos personales no se guardan en caché
        header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
    }

    // Límite de peticiones por dirección IP: frena fuerza bruta y abuso
    install(RateLimit) {
        global { rateLimiter(limit = 300, refillPeriod = 1.minutes) }
        register(RateLimitName("login")) { rateLimiter(limit = 20, refillPeriod = 1.minutes) }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                encodeDefaults = true
                ignoreUnknownKeys = true
            },
        )
    }

    // Un solo lugar traduce errores a respuestas: SIEMPRE el mismo formato { "error": { ... } }
    install(StatusPages) {
        exception<AppException> { call, e ->
            call.respond(
                HttpStatusCode.fromValue(e.status),
                ErrorResp(ErrorCuerpo(e.codigo, e.message ?: "Error", e.detalles)),
            )
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Error no controlado", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResp(ErrorCuerpo("ERROR_INTERNO", "Ocurrió un error inesperado. Intenta de nuevo más tarde.")),
            )
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "sano-movil"
            verifier(JWT.require(Algorithm.HMAC256(config.jwtSecreto)).withIssuer(config.jwtEmisor).build())
            validate { credencial ->
                if (credencial.payload.subject?.toIntOrNull() != null) JWTPrincipal(credencial.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResp(ErrorCuerpo("NO_AUTENTICADO", "Falta el token, es inválido o expiró")),
                )
            }
        }
    }

    routing {
        // Ruta informativa para abrir la URL base del servicio desde un navegador.
        // La API conserva sus endpoints versionados bajo /api/v1.
        get("/") {
            call.respondText("SanoMovil API activa. Comprobación de salud: /api/v1/salud")
        }

        // Versionado por ruta: /api/v1. Un cambio que rompa el contrato saldría como /api/v2.
        route("/api/v1") {
            rutasSalud(servicios.salud)
            rutasAuthPublicas(servicios.auth)
            authenticate("auth-jwt") {
                rutasAuthPrivadas(servicios.auth)
                rutasEquipos(servicios.equipos, servicios.revisiones)
                rutasRevisiones(servicios.revisiones)
                rutasModelos(servicios.modelos)
            }
        }
    }
}
