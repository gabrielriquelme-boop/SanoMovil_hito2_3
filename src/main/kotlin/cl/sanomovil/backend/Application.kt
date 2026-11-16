package cl.sanomovil.backend

import cl.sanomovil.backend.config.Config
import cl.sanomovil.backend.presentation.configurar
import cl.sanomovil.backend.repository.AccesoRepository
import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.EquipoRepository
import cl.sanomovil.backend.repository.ModeloRepository
import cl.sanomovil.backend.repository.RevisionRepository
import cl.sanomovil.backend.repository.UsuarioRepository
import cl.sanomovil.backend.security.ProveedorTokens
import cl.sanomovil.backend.service.AuthService
import cl.sanomovil.backend.service.EquipoService
import cl.sanomovil.backend.service.ModeloService
import cl.sanomovil.backend.service.RevisionService
import cl.sanomovil.backend.service.SaludService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/** Todos los servicios de negocio, listos para inyectarse en las rutas. */
class Servicios(
    val auth: AuthService,
    val equipos: EquipoService,
    val revisiones: RevisionService,
    val modelos: ModeloService,
    val salud: SaludService,
)

fun main() {
    val config = Config.cargar()
    val bd = BaseDeDatos(config)

    // Capa de datos
    val usuarios = UsuarioRepository()
    val equipos = EquipoRepository()
    val revisiones = RevisionRepository()
    val modelos = ModeloRepository()
    val accesos = AccesoRepository()

    // Capa de negocio
    val servicios = Servicios(
        auth = AuthService(bd, usuarios, accesos, ProveedorTokens(config)),
        equipos = EquipoService(bd, equipos, modelos),
        revisiones = RevisionService(bd, equipos, revisiones),
        modelos = ModeloService(bd, modelos),
        salud = SaludService(bd),
    )

    // Administrador inicial (solo si está definido en el .env)
    val adminCorreo = config.adminCorreo
    val adminClave = config.adminClave
    if (adminCorreo != null && adminClave != null) {
        servicios.auth.asegurarAdmin(adminCorreo, adminClave)
    }

    Runtime.getRuntime().addShutdownHook(Thread { bd.close() })

    // Capa de presentación: el servidor HTTP
    embeddedServer(Netty, port = config.puerto, host = "0.0.0.0") {
        configurar(config, servicios)
    }.start(wait = true)
}
