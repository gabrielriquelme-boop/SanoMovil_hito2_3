package cl.sanomovil.backend.service

import cl.sanomovil.backend.domain.Accion
import cl.sanomovil.backend.domain.Conflicto
import cl.sanomovil.backend.domain.DemasiadosIntentos
import cl.sanomovil.backend.domain.NoAutenticado
import cl.sanomovil.backend.domain.NoEncontrado
import cl.sanomovil.backend.domain.Rol
import cl.sanomovil.backend.domain.Sesion
import cl.sanomovil.backend.domain.Usuario
import cl.sanomovil.backend.domain.Valores
import cl.sanomovil.backend.repository.AccesoRepository
import cl.sanomovil.backend.repository.BaseDeDatos
import cl.sanomovil.backend.repository.UsuarioRepository
import cl.sanomovil.backend.security.Hasher
import cl.sanomovil.backend.security.ProveedorTokens
import java.sql.SQLException

/** CAPA DE NEGOCIO: registro, inicio de sesión y eliminación de cuenta. */
/** Hash falso que se usa solo para igualar los tiempos de respuesta cuando el correo no existe. */
private val HASH_FALSO: String by lazy { Hasher.hash("clave-falsa-que-nunca-se-usa-123") }

class AuthService(
    private val bd: BaseDeDatos,
    private val usuarios: UsuarioRepository,
    private val accesos: AccesoRepository,
    private val tokens: ProveedorTokens,
) {
    private enum class Resultado { OK, INCORRECTO, BLOQUEADO }

    fun registrar(datos: DatosRegistro): Usuario {
        val correo = datos.correo.trim().lowercase()
        val nombre = datos.nombre.trim()
        val errores = Errores()
        errores.exigir(Validador.correo(correo), "correo: formato inválido")
        errores.exigir(nombre.length in 2..60, "nombre: debe tener entre 2 y 60 caracteres")
        errores.exigir(Validador.clave(datos.clave), "clave: entre 8 y 72 caracteres, con al menos una letra y un número")
        errores.lanzarSiHay()

        val hash = Hasher.hash(datos.clave)
        return try {
            bd.transaccion { c ->
                if (usuarios.buscarPorCorreo(c, correo) != null) {
                    throw Conflicto("CORREO_EN_USO", "Ese correo ya está registrado")
                }
                val id = usuarios.crear(c, correo, nombre, hash, Rol.USUARIO)
                accesos.registrar(c, id, Accion.REGISTRO)
                usuarios.buscarPorId(c, id) ?: throw IllegalStateException("No se pudo leer el usuario recién creado")
            }
        } catch (e: SQLException) {
            // 23505 = violación de restricción UNIQUE (dos registros simultáneos con el mismo correo)
            if (e.sqlState == "23505") throw Conflicto("CORREO_EN_USO", "Ese correo ya está registrado")
            throw e
        }
    }

    fun iniciarSesion(datos: DatosLogin): Sesion {
        val correo = datos.correo.trim().lowercase()
        // El intento (bueno o malo) se registra y se guarda ANTES de lanzar el error,
        // por eso los errores se lanzan fuera de la transacción.
        val (resultado, usuario) = bd.transaccion { c ->
            val encontrado = usuarios.buscarPorCorreo(c, correo)
            val bloqueado = encontrado != null &&
                accesos.contarFallidosRecientes(c, encontrado.id, Valores.VENTANA_BLOQUEO_MIN) >= Valores.MAX_INTENTOS_LOGIN
            if (bloqueado) {
                Pair(Resultado.BLOQUEADO, null as Usuario?)
            } else {
                // Se verifica un hash SIEMPRE (aunque el correo no exista) para que la respuesta
                // demore lo mismo y no se pueda descubrir qué correos están registrados.
                val claveOk = Hasher.verificar(datos.clave, encontrado?.hashClave ?: HASH_FALSO)
                val correcto = claveOk && encontrado != null
                accesos.registrar(c, encontrado?.id, if (correcto) Accion.LOGIN_OK else Accion.LOGIN_FALLIDO)
                Pair(if (correcto) Resultado.OK else Resultado.INCORRECTO, if (correcto) encontrado else null)
            }
        }
        when (resultado) {
            Resultado.BLOQUEADO -> throw DemasiadosIntentos(
                "Demasiados intentos fallidos. Espera ${Valores.VENTANA_BLOQUEO_MIN} minutos y vuelve a intentar",
            )
            Resultado.INCORRECTO -> throw NoAutenticado("Correo o clave incorrectos")
            Resultado.OK -> Unit
        }
        val usuarioOk = usuario ?: throw NoAutenticado("Correo o clave incorrectos")
        val (token, expiraEnSeg) = tokens.emitir(usuarioOk)
        return Sesion(usuarioOk, token, expiraEnSeg)
    }

    fun perfil(usuarioId: Int): Usuario =
        bd.transaccion { c -> usuarios.buscarPorId(c, usuarioId) } ?: throw NoEncontrado("Usuario no encontrado")

    /** Derecho a eliminar los datos (Ley 21.719): borra la cuenta y, en cascada, sus equipos y revisiones. */
    fun eliminarCuenta(usuarioId: Int) {
        val borrados = bd.transaccion { c ->
            accesos.registrar(c, null, Accion.ELIMINAR_CUENTA)
            usuarios.eliminar(c, usuarioId)
        }
        if (borrados == 0) throw NoEncontrado("Usuario no encontrado")
    }

    /** Crea el administrador inicial a partir del .env (si no existe). */
    fun asegurarAdmin(correo: String, clave: String) {
        val correoNormal = correo.trim().lowercase()
        bd.transaccion { c ->
            if (usuarios.buscarPorCorreo(c, correoNormal) == null) {
                usuarios.crear(c, correoNormal, "Administrador", Hasher.hash(clave), Rol.ADMIN)
            }
        }
    }
}
