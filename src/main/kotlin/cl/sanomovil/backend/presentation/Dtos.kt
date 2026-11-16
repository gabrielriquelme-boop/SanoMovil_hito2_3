package cl.sanomovil.backend.presentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * CAPA DE PRESENTACIÓN: contratos JSON de la API (lo que entra y lo que sale).
 * Los nombres del JSON van en snake_case; en el código usamos camelCase.
 */

// ---------- Entrada (request) ----------
@Serializable data class RegistroReq(val correo: String, val nombre: String, val clave: String)
@Serializable data class LoginReq(val correo: String, val clave: String)

@Serializable
data class EquipoReq(
    val tipo: String,
    val marca: String,
    val modelo: String,
    val apodo: String? = null,
    @SerialName("modelo_id") val modeloId: Int? = null,
)

@Serializable
data class HallazgoReq(val codigo: String, val nivel: String, val titulo: String, val detalle: String? = null)

@Serializable
data class RevisionReq(
    val puntaje: Int,
    val nivel: String,
    @SerialName("bateria_pct") val bateriaPct: Int? = null,
    @SerialName("espacio_libre_pct") val espacioLibrePct: Int? = null,
    val nota: String? = null,
    val hallazgos: List<HallazgoReq> = emptyList(),
)

@Serializable data class NotaReq(val nota: String? = null)

@Serializable
data class ModeloReq(
    val marca: String,
    val nombre: String,
    @SerialName("anio_lanzamiento") val anioLanzamiento: Int? = null,
    val notas: String? = null,
)

// ---------- Salida (response) ----------
@Serializable
data class UsuarioResp(
    val id: Int,
    val correo: String,
    val nombre: String,
    val rol: String,
    @SerialName("creado_en") val creadoEn: String,
)

@Serializable
data class LoginResp(
    val token: String,
    val tipo: String = "Bearer",
    @SerialName("expira_en_seg") val expiraEnSeg: Long,
    val usuario: UsuarioResp,
)

@Serializable
data class EquipoResp(
    val id: Int,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val apodo: String?,
    @SerialName("modelo_id") val modeloId: Int?,
    @SerialName("creado_en") val creadoEn: String,
)

@Serializable
data class HallazgoResp(val id: Int, val codigo: String, val nivel: String, val titulo: String, val detalle: String?)

/** Versión resumida (para listados): no incluye los hallazgos. */
@Serializable
data class RevisionResumenResp(
    val id: Int,
    @SerialName("equipo_id") val equipoId: Int,
    val puntaje: Int,
    val nivel: String,
    @SerialName("bateria_pct") val bateriaPct: Int?,
    @SerialName("espacio_libre_pct") val espacioLibrePct: Int?,
    val nota: String?,
    @SerialName("creado_en") val creadoEn: String,
)

@Serializable
data class RevisionResp(
    val id: Int,
    @SerialName("equipo_id") val equipoId: Int,
    val puntaje: Int,
    val nivel: String,
    @SerialName("bateria_pct") val bateriaPct: Int?,
    @SerialName("espacio_libre_pct") val espacioLibrePct: Int?,
    val nota: String?,
    @SerialName("creado_en") val creadoEn: String,
    val hallazgos: List<HallazgoResp>,
)

@Serializable
data class ModeloResp(
    val id: Int,
    val marca: String,
    val nombre: String,
    @SerialName("anio_lanzamiento") val anioLanzamiento: Int?,
    val notas: String?,
)

@Serializable
data class SaludResp(
    val estado: String,
    @SerialName("base_de_datos") val baseDeDatos: String,
    val version: String,
)

// ---------- Errores: SIEMPRE el mismo formato ----------
@Serializable data class ErrorCuerpo(val codigo: String, val mensaje: String, val detalles: List<String> = emptyList())
@Serializable data class ErrorResp(val error: ErrorCuerpo)
