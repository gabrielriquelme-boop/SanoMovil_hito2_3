package com.example.sanomovil001

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Resultado de una llamada al servidor. [Fallo] con código 0 significa "no hay conexión". */
internal sealed interface RespuestaApi {
    data class Ok(val codigo: Int, val cuerpo: String) : RespuestaApi
    data class Fallo(val codigo: Int, val mensaje: String) : RespuestaApi
}

/** Una revisión guardada en el servidor, lista para mostrarse. */
data class FilaHistorial(val puntaje: Int, val nivel: Nivel, val fecha: String)

/**
 * CAPA DE ACCESO AL SERVIDOR (cliente de la API REST del Hito 2).
 * Las pantallas NO hablan HTTP: le piden las cosas a este objeto. Aquí viven la dirección del servidor,
 * el token de sesión y la traducción de los errores del servidor a mensajes para la persona.
 */
internal object Api {
    /** CAMBIA ESTA DIRECCIÓN por la de tu servicio en Render. Debe terminar en /api/v1 y sin barra al final. */
    const val URL_BASE = "https://sanomovil-hito2-3.onrender.com/api/v1"

    private const val ARCHIVO = "sano_api"
    private const val CLAVE_TOKEN = "token"
    private const val CLAVE_ESTE = "este_id"
    private const val APODO_ESTE = "_este_"   // marca al equipo que representa a ESTE teléfono

    private var appCtx: Context? = null

    @Volatile
    private var token: String? = null

    /** Pasa a true cuando el servidor dice que la sesión expiró; la pantalla principal vuelve entonces al inicio de sesión. */
    var sesionExpirada by mutableStateOf(false)

    fun iniciar(ctx: Context) {
        appCtx = ctx.applicationContext
        token = prefs()?.getString(CLAVE_TOKEN, null)?.takeIf { it.length in 20..2000 }
    }

    private fun prefs() = appCtx?.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun hayToken(): Boolean = token != null

    fun guardarToken(t: String) {
        token = t
        prefs()?.edit()?.putString(CLAVE_TOKEN, t)?.apply()
    }

    fun cerrarSesion() {
        token = null
        prefs()?.edit()?.clear()?.apply()
    }

    // ------------------------------------------------------------------ HTTP

    private suspend fun llamar(
        metodo: String,
        ruta: String,
        cuerpo: JSONObject? = null,
        conToken: Boolean = true,
    ): RespuestaApi {
        if (URL_BASE.contains("CAMBIA-ESTO")) {
            return RespuestaApi.Fallo(0, "Falta configurar la dirección del servidor (archivo Api.kt).")
        }
        return withContext(Dispatchers.IO) {
            var conexion: HttpURLConnection? = null
            try {
                val c = URL(URL_BASE + ruta).openConnection() as HttpURLConnection
                conexion = c
                c.requestMethod = metodo
                // El plan gratuito de Render se duerme y tarda cerca de un minuto en despertar: se da margen.
                c.connectTimeout = 30_000
                c.readTimeout = 75_000
                c.setRequestProperty("Accept", "application/json")
                val t = token
                if (conToken && t != null) c.setRequestProperty("Authorization", "Bearer $t")
                if (cuerpo != null) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    c.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                }
                val codigo = c.responseCode
                val flujo = if (codigo in 200..299) c.inputStream else c.errorStream
                val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (codigo in 200..299) {
                    RespuestaApi.Ok(codigo, texto)
                } else {
                    if (codigo == 401 && conToken && t != null) {
                        // Token vencido o inválido: se borra y se avisa para volver a iniciar sesión.
                        cerrarSesion()
                        sesionExpirada = true
                    }
                    RespuestaApi.Fallo(codigo, mensajeDeError(codigo, texto))
                }
            } catch (e: IOException) {
                RespuestaApi.Fallo(0, "No pudimos conectar con el servidor. Revisa tu internet e inténtalo de nuevo.")
            } finally {
                conexion?.disconnect()
            }
        }
    }

    /** Traduce el error técnico del servidor a una frase para la persona. */
    private fun mensajeDeError(codigo: Int, texto: String): String {
        val error = runCatching { JSONObject(texto).getJSONObject("error") }.getOrNull()
        val mensaje = error?.optString("mensaje").orEmpty()
        val detalles = error?.optJSONArray("detalles")
        val lista = if (detalles != null) {
            (0 until detalles.length()).map { detalles.optString(it) }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        return when {
            codigo == 422 && lista.isNotEmpty() -> lista.joinToString(" ")
            codigo == 401 -> mensaje.ifBlank { "Tu sesión no es válida. Vuelve a iniciar sesión." }
            codigo == 404 -> "No encontramos lo que buscabas."
            codigo == 409 || codigo == 429 -> mensaje.ifBlank { "No se pudo completar la acción." }
            codigo in 500..599 -> "El servidor tuvo un problema. Inténtalo de nuevo en un momento."
            else -> mensaje.ifBlank { "Algo salió mal (código $codigo). Inténtalo de nuevo." }
        }
    }

    // ------------------------------------------------------------------ Cuenta

    suspend fun login(correo: String, clave: String): RespuestaApi =
        llamar("POST", "/auth/login", JSONObject().put("correo", correo).put("clave", clave), conToken = false)

    suspend fun registro(correo: String, nombre: String, clave: String): RespuestaApi =
        llamar(
            "POST", "/auth/registro",
            JSONObject().put("correo", correo).put("nombre", nombre).put("clave", clave),
            conToken = false,
        )

    // ------------------------------------------------------------------ Equipos

    suspend fun listarEquipos(): RespuestaApi = llamar("GET", "/equipos")

    suspend fun crearEquipo(tipo: TipoDispositivo, marca: String, modelo: String, apodo: String): RespuestaApi {
        val cuerpo = JSONObject()
            .put("tipo", tipo.name.lowercase())
            .put("marca", marca)
            .put("modelo", modelo)
        if (apodo.isNotBlank()) cuerpo.put("apodo", apodo)
        return llamar("POST", "/equipos", cuerpo)
    }

    suspend fun borrarEquipo(id: String): RespuestaApi {
        val n = id.toIntOrNull() ?: return RespuestaApi.Fallo(400, "Ese equipo no está guardado en tu cuenta.")
        return llamar("DELETE", "/equipos/$n")
    }

    /** Convierte la respuesta de GET /equipos en la lista que usa la pantalla (sin el equipo "este teléfono"). */
    fun aDispositivos(texto: String): List<DispositivoFamilia> {
        val arr = runCatching { JSONArray(texto) }.getOrNull() ?: return emptyList()
        val res = mutableListOf<DispositivoFamilia>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val apodo = if (o.isNull("apodo")) "" else o.optString("apodo")
            if (apodo == APODO_ESTE) continue
            val tipo = TipoDispositivo.values().firstOrNull { it.name.equals(o.optString("tipo"), ignoreCase = true) } ?: continue
            res.add(DispositivoFamilia(o.optInt("id").toString(), tipo, o.optString("marca"), o.optString("modelo"), apodo))
        }
        return res
    }

    // ------------------------------------------------------------------ Revisiones de ESTE teléfono

    /** Busca (o crea) en el servidor el equipo que representa a este teléfono. Devuelve su id o un mensaje de error. */
    private suspend fun idEsteTelefono(): Pair<Int?, String?> {
        val guardado = prefs()?.getInt(CLAVE_ESTE, 0) ?: 0
        if (guardado > 0) return Pair(guardado, null)

        when (val lista = llamar("GET", "/equipos")) {
            is RespuestaApi.Fallo -> return Pair(null, lista.mensaje)
            is RespuestaApi.Ok -> {
                val arr = runCatching { JSONArray(lista.cuerpo) }.getOrNull() ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    if (!o.isNull("apodo") && o.optString("apodo") == APODO_ESTE) {
                        val id = o.optInt("id")
                        if (id > 0) {
                            prefs()?.edit()?.putInt(CLAVE_ESTE, id)?.apply()
                            return Pair(id, null)
                        }
                    }
                }
            }
        }

        val info = Dispositivo.info()
        val cuerpo = JSONObject()
            .put("tipo", "telefono")
            .put("marca", limpiar(info.marca.replaceFirstChar { it.uppercase() }, 40, "Android"))
            .put("modelo", limpiar(info.modelo, 60, "Telefono"))
            .put("apodo", APODO_ESTE)
        return when (val creado = llamar("POST", "/equipos", cuerpo)) {
            is RespuestaApi.Fallo -> Pair(null, creado.mensaje)
            is RespuestaApi.Ok -> {
                val id = runCatching { JSONObject(creado.cuerpo).getInt("id") }.getOrDefault(0)
                if (id > 0) {
                    prefs()?.edit()?.putInt(CLAVE_ESTE, id)?.apply()
                    Pair(id, null)
                } else {
                    Pair(null, "El servidor respondió algo inesperado.")
                }
            }
        }
    }

    /** Deja solo los caracteres que acepta el servidor (letras, números, espacio, punto, guion). */
    private fun limpiar(texto: String, maximo: Int, porDefecto: String): String =
        texto.replace(Regex("[^\\p{L}\\p{N} ._-]"), " ").trim().take(maximo).trim().ifBlank { porDefecto }

    private fun nombreNivel(n: Nivel): String = n.name.lowercase()

    private fun cuerpoRevision(ctx: Context, r: ResultadoRevision): JSONObject {
        val bateria = Dispositivo.bateria(ctx)?.porcentaje?.takeIf { it in 0..100 }
        val almacenamiento = Dispositivo.almacenamiento()
        val libre = almacenamiento?.let { if (it.total > 0) (it.libre * 100 / it.total).toInt() else null }
            ?.takeIf { it in 0..100 }

        val hallazgos = JSONArray()
        r.hallazgos.take(30).forEachIndexed { i, h ->
            val codigo = (h.guiaId ?: "hallazgo_$i").lowercase()
                .replace('-', '_')
                .replace(Regex("[^a-z0-9_]"), "")
                .take(40)
                .ifBlank { "hallazgo_$i" }
            hallazgos.put(
                JSONObject()
                    .put("codigo", codigo)
                    .put("nivel", nombreNivel(h.nivel))
                    .put("titulo", h.titulo.trim().take(120).ifBlank { "Hallazgo" })
                    .put("detalle", h.detalle.trim().take(500)),
            )
        }
        val cuerpo = JSONObject()
            .put("puntaje", r.puntaje.coerceIn(0, 100))
            .put("nivel", nombreNivel(r.nivel))
            .put("hallazgos", hallazgos)
        if (bateria != null) cuerpo.put("bateria_pct", bateria)
        if (libre != null) cuerpo.put("espacio_libre_pct", libre)
        return cuerpo
    }

    /** Guarda en el servidor la revisión recién hecha. Devuelve null si salió bien, o el mensaje de error. */
    suspend fun guardarRevision(ctx: Context, r: ResultadoRevision): String? {
        repeat(2) { intento ->
            val (id, problema) = idEsteTelefono()
            if (id == null) return problema ?: "No se pudo preparar tu equipo en el servidor."
            when (val resp = llamar("POST", "/equipos/$id/revisiones", cuerpoRevision(ctx, r))) {
                is RespuestaApi.Ok -> return null
                is RespuestaApi.Fallo -> {
                    if (resp.codigo == 404 && intento == 0) {
                        // El equipo guardado ya no existe en el servidor: se olvida y se vuelve a preparar.
                        prefs()?.edit()?.remove(CLAVE_ESTE)?.apply()
                    } else {
                        return resp.mensaje
                    }
                }
            }
        }
        return "No se pudo guardar la revisión."
    }

    /** Lee del servidor las últimas revisiones de este teléfono. Devuelve la lista y, si falló, el mensaje de error. */
    suspend fun historialDeEsteTelefono(): Pair<List<FilaHistorial>, String?> {
        val (id, problema) = idEsteTelefono()
        if (id == null) return Pair(emptyList(), problema)
        return when (val r = llamar("GET", "/equipos/$id/revisiones?limite=10")) {
            is RespuestaApi.Fallo -> {
                if (r.codigo == 404) {
                    prefs()?.edit()?.remove(CLAVE_ESTE)?.apply()
                    Pair(emptyList(), null)
                } else {
                    Pair(emptyList(), r.mensaje)
                }
            }
            is RespuestaApi.Ok -> {
                val arr = runCatching { JSONArray(r.cuerpo) }.getOrNull() ?: JSONArray()
                val filas = mutableListOf<FilaHistorial>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val nivel = Nivel.values().firstOrNull { it.name.equals(o.optString("nivel"), ignoreCase = true) } ?: Nivel.OK
                    filas.add(FilaHistorial(o.optInt("puntaje"), nivel, formatearFecha(o.optString("creado_en"))))
                }
                Pair(filas, null)
            }
        }
    }

    private fun formatearFecha(iso: String): String = runCatching {
        java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.parse(iso))
    }.getOrDefault(iso.take(16))
}

/** Cuentas reales: el servicio de autenticación que usa el servidor del Hito 2 (token JWT). */
internal object AutenticacionApi : ProveedorAutenticacion {
    override val esDemo = false

    private fun aSesion(cuerpo: String): Sesion? = runCatching {
        val o = JSONObject(cuerpo)
        Api.guardarToken(o.getString("token"))
        val u = o.getJSONObject("usuario")
        Sesion(u.getString("correo"), u.getString("nombre"))
    }.getOrNull()

    override suspend fun iniciarSesion(correo: String, clave: String): ResultadoAuth {
        val problema = Autenticacion.revisar(correo, clave)
        if (problema != null) return ResultadoAuth.Error(problema)
        return when (val r = Api.login(correo, clave)) {
            is RespuestaApi.Fallo -> ResultadoAuth.Error(r.mensaje)
            is RespuestaApi.Ok -> {
                val s = aSesion(r.cuerpo)
                if (s != null) ResultadoAuth.Exito(s) else ResultadoAuth.Error("El servidor respondió algo inesperado.")
            }
        }
    }

    override suspend fun registrar(correo: String, clave: String): ResultadoAuth {
        val problema = Autenticacion.revisar(correo, clave)
        if (problema != null) return ResultadoAuth.Error(problema)
        var nombre = Autenticacion.nombreDesdeCorreo(correo)
        if (nombre.length < 2) nombre = "Usuario"
        return when (val r = Api.registro(correo, nombre, clave)) {
            is RespuestaApi.Fallo -> ResultadoAuth.Error(r.mensaje)
            is RespuestaApi.Ok -> iniciarSesion(correo, clave)   // la cuenta ya existe: se entra de inmediato
        }
    }

    override suspend fun conGoogle(actividad: Activity): ResultadoAuth =
        ResultadoAuth.Error("Entrar con Google aún no está disponible. Usa tu correo y contraseña.")

    override fun cerrarSesion() = Api.cerrarSesion()
}