package com.example.sanomovil001

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class SubEcosistema { AGREGAR, FICHA }

/** Un equipo tal como se muestra en las tarjetas (este teléfono o uno registrado). */
private data class ItemEco(val id: String, val nombre: String, val subtitulo: String)

/**
 * Mis equipos: una zona "Mis dispositivos" y, abajo, los equipos disponibles.
 * Se arrastra un equipo a la zona (o simplemente se toca) y desde ahí se abre su ficha.
 *  sub == null -> zona y bandeja; AGREGAR -> formulario; FICHA -> ficha del equipo elegido.
 */
@Composable
fun PantallaEcosistema(
    sub: SubEcosistema?,
    dispositivoId: String?,
    ultima: UltimaRevision?,
    onAgregar: () -> Unit,
    onAbrir: (String) -> Unit,
    onListo: () -> Unit,
    onGuia: (String) -> Unit,
    onVolver: () -> Unit,
    onPestana: (Pestana) -> Unit
) {
    val context = LocalContext.current
    // La lista se lee del teléfono y, si hay cuenta, se actualiza desde el servidor cada vez que se cambia de subpantalla.
    var lista by remember { mutableStateOf(Ecosistema.leer(context)) }
    var cargando by remember { mutableStateOf(false) }
    var errorRed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sub) {
        lista = Ecosistema.leer(context)
        if (Ecosistema.conServidor()) {
            cargando = true
            errorRed = Ecosistema.sincronizar(context)
            lista = Ecosistema.leer(context)
            cargando = false
        }
    }

    when (sub) {
        null -> ZonaEquipos(lista, cargando, errorRed, onAgregar, onAbrir, onVolver, onPestana)
        SubEcosistema.AGREGAR -> FormularioDispositivo(onGuardado = onListo, onVolver = onVolver)
        SubEcosistema.FICHA -> FichaDispositivo(dispositivoId, lista, ultima, onListo, onGuia, onVolver)
    }
}

// ------------------------------------------------------- Zona y bandeja

@Composable
private fun ZonaEquipos(
    lista: List<DispositivoFamilia>,
    cargando: Boolean,
    errorRed: String?,
    onAgregar: () -> Unit,
    onAbrir: (String) -> Unit,
    onVolver: () -> Unit,
    onPestana: (Pestana) -> Unit
) {
    val context = LocalContext.current
    val tacto = LocalHapticFeedback.current
    val info = remember { Dispositivo.info() }

    val items = remember(lista) {
        listOf(ItemEco(Ecosistema.ESTE_EQUIPO, "Este teléfono", info.nombre)) +
            lista.map { ItemEco(it.id, it.nombre, "${it.tipo.titulo} · ${it.marca} ${it.modelo}") }
    }
    var colocados by remember { mutableStateOf(Ecosistema.colocados(context)) }

    // Estado del arrastre (posiciones en pantalla)
    var arrastrandoId by remember { mutableStateOf<String?>(null) }
    var inicio by remember { mutableStateOf(Offset.Zero) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }
    var zona by remember { mutableStateOf<Rect?>(null) }
    var origenCaja by remember { mutableStateOf(Offset.Zero) }

    fun colocar(id: String) {
        Ecosistema.colocar(context, id)
        colocados = Ecosistema.colocados(context)
        tacto.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun quitar(id: String) {
        Ecosistema.quitar(context, id)
        colocados = Ecosistema.colocados(context)
    }

    fun soltar() {
        val id = arrastrandoId
        val z = zona
        if (id != null && z != null) {
            val centro = inicio + desplazamiento + Offset(tamano.width / 2f, tamano.height / 2f)
            if (z.contains(centro)) colocar(id)
        }
        arrastrandoId = null
        desplazamiento = Offset.Zero
    }

    val enZona = items.filter { it.id in colocados }.sortedBy { colocados.indexOf(it.id) }
    val enBandeja = items.filter { it.id !in colocados }
    val centroActual = inicio + desplazamiento + Offset(tamano.width / 2f, tamano.height / 2f)
    val sobreZona = arrastrandoId != null && zona?.contains(centroActual) == true
    val forma = RoundedCornerShape(24.dp)

    PantallaBase("Mis equipos", onVolver, pestana = Pestana.EQUIPOS, onPestana = onPestana) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { origenCaja = it.positionInWindow() }
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Txt("Arrastra un equipo a tu zona, o tócalo.", 15, Tema.TextoSuave)
                if (cargando) Txt("Sincronizando con el servidor…", 14, Tema.TextoSuave)
                errorRed?.let { Txt(it, 14, Tema.Peligro, negrita = true) }

                // ---- Zona "Mis dispositivos"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { zona = it.boundsInWindow() }
                        .background(if (sobreZona) Tema.fondoDe(Nivel.OK) else Tema.Tonal, forma)
                        .border(2.dp, if (sobreZona) Tema.Boton else Tema.Borde, forma)
                        .padding(14.dp)
                ) {
                    Txt("Mis dispositivos", 17, negrita = true)
                    if (enZona.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Txt(
                                "Suelta aquí un equipo para tenerlo siempre a mano.",
                                16, Tema.TextoSuave, centrado = true
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            enZona.forEach { item ->
                                TarjetaColocada(item, onAbrir = { onAbrir(item.id) }, onQuitar = { quitar(item.id) })
                            }
                        }
                    }
                }

                // ---- Bandeja de equipos disponibles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Txt("Disponibles", 14, Tema.TextoSuave)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        enBandeja.forEach { item ->
                            ChipArrastrable(
                                item = item,
                                oculto = arrastrandoId == item.id,
                                onTocar = { colocar(item.id) },
                                onInicio = { pos, tam ->
                                    arrastrandoId = item.id
                                    inicio = pos
                                    tamano = tam
                                    desplazamiento = Offset.Zero
                                },
                                onMover = { delta -> desplazamiento += delta },
                                onSoltar = { soltar() },
                                onCancelar = {
                                    arrastrandoId = null
                                    desplazamiento = Offset.Zero
                                }
                            )
                        }
                        if (lista.size < Ecosistema.MAX) {
                            Box(modifier = Modifier.width(150.dp)) {
                                BotonSano("＋ Agregar", color = Tema.BotonSecundario, compacto = true, onClick = onAgregar)
                            }
                        }
                    }
                    if (enBandeja.isEmpty()) {
                        Txt("Todos tus equipos ya están en tu zona.", 14, Tema.TextoSuave)
                    }
                }
            }

            // ---- Copia que sigue al dedo mientras se arrastra
            val enMovimiento = items.firstOrNull { it.id == arrastrandoId }
            if (enMovimiento != null) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (inicio.x + desplazamiento.x - origenCaja.x).roundToInt(),
                                (inicio.y + desplazamiento.y - origenCaja.y).roundToInt()
                            )
                        }
                        .width(CHIP_ANCHO)
                ) {
                    ChipVisual(enMovimiento, onTocar = null)
                }
            }
        }
    }
}

private val CHIP_ANCHO = 170.dp

/** Contenido de un equipo como "objeto" (se usa en la bandeja y en la copia que se arrastra). */
@Composable
private fun ChipVisual(item: ItemEco, onTocar: (() -> Unit)?) {
    Column(modifier = Modifier.tarjeta(onClick = onTocar)) {
        Txt(item.nombre, 16, negrita = true, maxLineas = 1)
        Txt(item.subtitulo, 12, Tema.TextoSuave, maxLineas = 1)
    }
}

@Composable
private fun ChipArrastrable(
    item: ItemEco,
    oculto: Boolean,
    onTocar: () -> Unit,
    onInicio: (Offset, IntSize) -> Unit,
    onMover: (Offset) -> Unit,
    onSoltar: () -> Unit,
    onCancelar: () -> Unit
) {
    var pos by remember { mutableStateOf(Offset.Zero) }
    var tam by remember { mutableStateOf(IntSize.Zero) }
    val alInicio by rememberUpdatedState(onInicio)
    val alMover by rememberUpdatedState(onMover)
    val alSoltar by rememberUpdatedState(onSoltar)
    val alCancelar by rememberUpdatedState(onCancelar)

    Box(
        modifier = Modifier
            .width(CHIP_ANCHO)
            .alpha(if (oculto) 0.25f else 1f)
            .onGloballyPositioned {
                pos = it.positionInWindow()
                tam = it.size
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { alInicio(pos, tam) },
                    onDrag = { change, cantidad ->
                        change.consume()
                        alMover(cantidad)
                    },
                    onDragEnd = { alSoltar() },
                    onDragCancel = { alCancelar() }
                )
            }
    ) {
        ChipVisual(item, onTocar)
    }
}

@Composable
private fun TarjetaColocada(item: ItemEco, onAbrir: () -> Unit, onQuitar: () -> Unit) {
    Row(modifier = Modifier.tarjeta(onClick = onAbrir), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Txt(item.nombre, 18, negrita = true)
            Txt(item.subtitulo, 14, Tema.TextoSuave)
        }
        TextButton(
            onClick = onQuitar,
            modifier = Modifier.semantics { contentDescription = "Quitar ${item.nombre} de mi zona" }
        ) {
            Txt("✕", 20, Tema.TextoSuave, negrita = true)
        }
    }
}

// ------------------------------------------------------------------ Ficha

@Composable
private fun FichaDispositivo(
    dispositivoId: String?,
    lista: List<DispositivoFamilia>,
    ultima: UltimaRevision?,
    onListo: () -> Unit,
    onGuia: (String) -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val info = remember { Dispositivo.info() }
    val esEste = dispositivoId == Ecosistema.ESTE_EQUIPO
    val d = lista.firstOrNull { it.id == dispositivoId }
    var confirmar by remember { mutableStateOf(false) }
    var errorBorrar by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Si el equipo ya no existe (por ejemplo, se acaba de eliminar), se vuelve a la zona.
    if (!esEste && d == null) {
        LaunchedEffect(Unit) { onListo() }
        return
    }

    val modelo = if (esEste) info.modelo else d?.modelo.orEmpty()
    val ficha = remember(modelo) { Catalogo.fichaPara(context, modelo) }

    PantallaBase(if (esEste) "Este teléfono" else d?.nombre.orEmpty(), onVolver) {
        ZonaScroll {
            if (esEste) {
                FilaDato("Equipo", info.nombre)
                FilaDato("Versión de Android", info.release)
                FilaDato(
                    "Última revisión",
                    if (ultima == null) "Aún sin revisar" else "${ultima.puntaje} de 100 · ${Escaner.etiquetaDe(ultima.nivel)}"
                )
                HistorialServidor()
            } else if (d != null) {
                FilaDato("Tipo", d.tipo.titulo)
                FilaDato("Marca y modelo", "${d.marca} ${d.modelo}")
                Column(modifier = Modifier.tarjeta()) {
                    Txt(
                        "Este equipo no se puede revisar en vivo desde aquí. Para hacerlo, instala Sano Móvil en él.",
                        15, Tema.TextoSuave
                    )
                }
            }

            Txt("Ficha del modelo", 18, negrita = true)
            if (ficha == null) {
                Column(modifier = Modifier.tarjeta()) {
                    Txt(
                        "Aún no tenemos datos verificados de este modelo. Mientras tanto, estas guías sirven para cualquier equipo.",
                        15
                    )
                }
            } else {
                ficha.nombre?.let { Txt(it, 17, Tema.Boton, negrita = true) }
                ficha.datos.forEach { (etiqueta, valor) -> FilaDato(etiqueta, valor) }
                ficha.alertas.forEach { a ->
                    Row(modifier = Modifier.tarjeta(fondo = Tema.fondoDe(a.nivel)), verticalAlignment = Alignment.Top) {
                        PuntoNivel(a.nivel)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Txt(a.titulo, 16, negrita = true)
                            Txt(a.detalle, 14)
                        }
                    }
                }
            }

            BotonSano("Guía: cuidar la batería", color = Tema.BotonSecundario, compacto = true) { onGuia("cuidar-bateria") }
            BotonSano("Guía: buscar actualizaciones", color = Tema.BotonSecundario, compacto = true) { onGuia("actualizar-sistema") }
            if (!esEste) {
                BotonSano("Quitar de mis equipos", color = Tema.Peligro, compacto = true) { confirmar = true }
            }
        }
    }

    if (confirmar && d != null) {
        DialogoSano(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Quitar ${d.nombre}?") },
            text = { Text("Solo se quita de esta lista. No se borra nada del equipo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    scope.launch {
                        if (Ecosistema.conServidor()) {
                            errorBorrar = Ecosistema.eliminarRemoto(context, d.id)
                        } else {
                            Ecosistema.eliminar(context, d.id)
                        }
                        if (errorBorrar == null) onListo()
                    }
                }) { Text("Sí, quitar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }

    errorBorrar?.let { mensaje ->
        DialogoMensaje(texto = mensaje, titulo = "No se pudo quitar el equipo") { errorBorrar = null }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column(modifier = Modifier.tarjeta()) {
        Txt(etiqueta, 14, Tema.TextoSuave)
        Txt(valor, 17, negrita = true)
    }
}

// -------------------------------------------------------------- Formulario

@Composable
private fun FormularioDispositivo(onGuardado: () -> Unit, onVolver: () -> Unit) {
    val context = LocalContext.current
    var tipo by rememberSaveable { mutableStateOf(TipoDispositivo.TABLET) }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var apodo by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PantallaBase("Agregar un equipo", onVolver, textoVolver = "← Cancelar") {
        ZonaScroll {
            Txt("¿Qué tipo de equipo es?", 16, negrita = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoDispositivo.values().forEach { t ->
                    BotonSano(
                        t.titulo, Modifier.weight(1f),
                        color = if (t == tipo) Tema.Boton else Tema.BotonSecundario,
                        compacto = true
                    ) { tipo = t }
                }
            }
            Campo("Marca (por ejemplo, Samsung)", marca) { marca = it }
            Campo("Modelo", modelo) { modelo = it }
            Txt("El modelo se ve en Ajustes > Acerca del teléfono.", 13, Tema.TextoSuave)
            Campo("Nombre para reconocerlo (opcional)", apodo) { apodo = it }

            error?.let { Txt(it, 15, Tema.Peligro, negrita = true) }

            if (guardando) Txt("Guardando… la primera vez puede tardar hasta un minuto.", 14, Tema.TextoSuave)

            BotonSano(if (guardando) "Guardando…" else "Guardar", activo = !guardando) {
                error = null
                guardando = true
                scope.launch {
                    error = if (Ecosistema.conServidor()) {
                        Ecosistema.agregarRemoto(context, tipo, marca, modelo, apodo)
                    } else {
                        Ecosistema.agregar(context, tipo, marca, modelo, apodo)
                    }
                    guardando = false
                    if (error == null) onGuardado()
                }
            }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= 40) onCambio(it) },
        label = { Text(etiqueta) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Historial de revisiones de ESTE teléfono, leído del servidor (con estados de carga, vacío y error). */
@Composable
private fun HistorialServidor() {
    if (!Api.hayToken()) {
        Txt("Inicia sesión desde tu perfil para guardar el historial de tus revisiones en tu cuenta.", 15, Tema.TextoSuave)
        return
    }
    var cargando by remember { mutableStateOf(true) }
    var problema by remember { mutableStateOf<String?>(null) }
    var filas by remember { mutableStateOf<List<FilaHistorial>>(emptyList()) }

    LaunchedEffect(Unit) {
        val (lista, error) = Api.historialDeEsteTelefono()
        filas = lista
        problema = error
        cargando = false
    }

    Txt("Historial guardado en tu cuenta", 18, negrita = true)
    val mensaje = problema
    when {
        cargando -> Txt("Cargando desde el servidor…", 15, Tema.TextoSuave)
        mensaje != null -> Txt(mensaje, 15, Tema.Peligro, negrita = true)
        filas.isEmpty() -> Txt("Todavía no hay revisiones guardadas. Haz una desde Inicio con «Escanear».", 15, Tema.TextoSuave)
        else -> filas.forEach { f -> FilaDato(f.fecha, "${f.puntaje} de 100 · ${Escaner.etiquetaDe(f.nivel)}") }
    }
}

