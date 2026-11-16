package cl.sanomovil.backend.presentation

import cl.sanomovil.backend.domain.Equipo
import cl.sanomovil.backend.domain.Hallazgo
import cl.sanomovil.backend.domain.Modelo
import cl.sanomovil.backend.domain.Revision
import cl.sanomovil.backend.domain.Sesion
import cl.sanomovil.backend.domain.Usuario
import cl.sanomovil.backend.service.DatosEquipo
import cl.sanomovil.backend.service.DatosHallazgo
import cl.sanomovil.backend.service.DatosLogin
import cl.sanomovil.backend.service.DatosModelo
import cl.sanomovil.backend.service.DatosRegistro
import cl.sanomovil.backend.service.DatosRevision

// ---------- JSON de entrada -> datos de la capa de negocio ----------
fun RegistroReq.aDatos() = DatosRegistro(correo, nombre, clave)
fun LoginReq.aDatos() = DatosLogin(correo, clave)
fun EquipoReq.aDatos() = DatosEquipo(tipo, marca, modelo, apodo, modeloId)
fun HallazgoReq.aDatos() = DatosHallazgo(codigo, nivel, titulo, detalle)
fun RevisionReq.aDatos() =
    DatosRevision(puntaje, nivel, bateriaPct, espacioLibrePct, nota, hallazgos.map { it.aDatos() })
fun ModeloReq.aDatos() = DatosModelo(marca, nombre, anioLanzamiento, notas)

// ---------- Entidades del dominio -> JSON de salida ----------
// Nota: el hash de la contraseña NUNCA sale en ninguna respuesta.
fun Usuario.aResp() = UsuarioResp(id, correo, nombre, rol, creadoEn.toString())
fun Sesion.aResp() = LoginResp(token = token, expiraEnSeg = expiraEnSeg, usuario = usuario.aResp())
fun Equipo.aResp() = EquipoResp(id, tipo, marca, modelo, apodo, modeloId, creadoEn.toString())
fun Hallazgo.aResp() = HallazgoResp(id, codigo, nivel, titulo, detalle)
fun Modelo.aResp() = ModeloResp(id, marca, nombre, anioLanzamiento, notas)
fun Revision.aResumen() =
    RevisionResumenResp(id, equipoId, puntaje, nivel, bateriaPct, espacioLibrePct, nota, creadoEn.toString())
fun Revision.aResp() =
    RevisionResp(id, equipoId, puntaje, nivel, bateriaPct, espacioLibrePct, nota, creadoEn.toString(), hallazgos.map { it.aResp() })
