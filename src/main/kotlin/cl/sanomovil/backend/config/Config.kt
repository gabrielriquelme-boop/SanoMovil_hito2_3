package cl.sanomovil.backend.config

import java.io.File

/**
 * Configuración del servicio. Todo lo sensible (claves, URL de la base) viene de
 * variables de entorno o del archivo .env, que NO se sube al repositorio.
 * Ver .env.example en la raíz del repositorio.
 */
data class Config(
    val puerto: Int,
    val dbUrl: String,
    val dbUsuario: String,
    val dbClave: String,
    val jwtSecreto: String,
    val jwtEmisor: String,
    val jwtDuracionMin: Long,
    val adminCorreo: String?,
    val adminClave: String?,
    val confiarProxy: Boolean,
) {
    companion object {
        fun cargar(): Config {
            val archivo = leerArchivoEnv()

            // Las variables de entorno del sistema tienen prioridad sobre el archivo .env
            fun valor(nombre: String): String? =
                System.getenv(nombre)?.takeIf { it.isNotBlank() } ?: archivo[nombre]?.takeIf { it.isNotBlank() }

            fun requerido(nombre: String): String =
                valor(nombre) ?: error("Falta la variable $nombre. Copia .env.example a .env y complétala.")

            val secreto = requerido("JWT_SECRET")
            require(secreto.length >= 32) { "JWT_SECRET debe tener al menos 32 caracteres." }

            return Config(
                puerto = valor("PORT")?.toIntOrNull() ?: 8080,
                dbUrl = requerido("DB_URL"),
                dbUsuario = requerido("DB_USER"),
                dbClave = requerido("DB_PASSWORD"),
                jwtSecreto = secreto,
                jwtEmisor = valor("JWT_ISSUER") ?: "sano-movil",
                jwtDuracionMin = valor("JWT_TTL_MINUTES")?.toLongOrNull() ?: 120L,
                adminCorreo = valor("ADMIN_EMAIL"),
                adminClave = valor("ADMIN_PASSWORD"),
                confiarProxy = valor("TRUST_PROXY")?.lowercase() == "true",
            )
        }

        // Busca el .env en la carpeta actual (backend/) o en la raíz del repositorio (../)
        private fun leerArchivoEnv(): Map<String, String> {
            val archivo = listOf(File(".env"), File("../.env")).firstOrNull { it.isFile } ?: return emptyMap()
            return archivo.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .associate { linea ->
                    linea.substringBefore("=").trim() to linea.substringAfter("=").trim().trim('"')
                }
        }
    }
}
