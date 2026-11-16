package cl.sanomovil.backend.security

import at.favre.lib.crypto.bcrypt.BCrypt
import cl.sanomovil.backend.config.Config
import cl.sanomovil.backend.domain.Usuario
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/** Hash de contraseñas con BCrypt (costo 12). Nunca se guarda la contraseña en texto plano. */
object Hasher {
    fun hash(clave: String): String = BCrypt.withDefaults().hashToString(12, clave.toCharArray())

    fun verificar(clave: String, hash: String): Boolean =
        BCrypt.verifyer().verify(clave.toCharArray(), hash).verified
}

/** Emite tokens JWT firmados (HS256). El secreto viene del .env, nunca del código. */
class ProveedorTokens(private val config: Config) {
    private val algoritmo = Algorithm.HMAC256(config.jwtSecreto)

    /** Devuelve el token y su duración en segundos. */
    fun emitir(usuario: Usuario): Pair<String, Long> {
        val ahora = System.currentTimeMillis()
        val duracionMs = config.jwtDuracionMin * 60_000L
        val token = JWT.create()
            .withIssuer(config.jwtEmisor)
            .withSubject(usuario.id.toString())
            .withClaim("rol", usuario.rol)
            .withIssuedAt(Date(ahora))
            .withExpiresAt(Date(ahora + duracionMs))
            .sign(algoritmo)
        return Pair(token, config.jwtDuracionMin * 60L)
    }
}
