package mx.cecapi.escritorio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant

private val forma = RoundedCornerShape(16.dp)

// ------------------------------------------------------------------------------------ Políticas

/**
 * Políticas por partes, como en la web y en el teléfono: Escuchar, Anterior, Repetir y Siguiente.
 * Para aceptar hay que llegar a la última parte y declarar 18 años o más. "Rechazar" y "Después"
 * dejan el asistente en pausa.
 */
@Composable
fun PantallaPoliticas(voz: Voz, alAceptar: (Aceptacion) -> Unit) {
    val lector = remember { LectorDePoliticas() }
    var cambio by remember { mutableIntStateOf(0) }
    var conFoco by remember { mutableStateOf(false) }
    var escuchar by remember { mutableStateOf(false) }
    var mayor by remember { mutableStateOf(false) }
    var requisito by remember { mutableStateOf<String?>(null) }
    var pausa by remember { mutableStateOf<String?>(null) }
    val focoTitulo = remember { FocusRequester() }
    val desplazamiento = rememberScrollState()

    // `cambio` sube cada vez que se avanza o se repite: así la pantalla se vuelve a dibujar.
    val parte = remember(cambio) { Politicas.partes[lector.indice] }
    val total = Politicas.partes.size

    fun mostrar(foco: Boolean) {
        cambio++
        conFoco = foco
        if (escuchar) voz.decir("Parte ${lector.indice + 1} de $total. " + Politicas.partes[lector.indice].textoParaVoz)
    }

    LaunchedEffect(cambio) {
        if (conFoco) runCatching { focoTitulo.requestFocus() }
    }

    Box(Modifier.fillMaxSize().verticalScroll(desplazamiento), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 900.dp).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Antes de empezar: políticas de uso y privacidad", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })

            val motivo = pausa
            if (motivo != null) {
                Text("El asistente está en pausa", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(motivo, style = MaterialTheme.typography.bodyLarge)
                BotonGrande("Leer las políticas", onClick = { pausa = null; mostrar(true) }, principal = true)
                return@Column
            }

            Text(
                "Léelas por partes con los botones. Para usar el asistente hay que llegar al final, declarar que eres mayor de edad y aceptarlas. Pasar las partes sin leerlas no cuenta como aceptar.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Parte ${lector.indice + 1} de $total", style = MaterialTheme.typography.titleLarge, color = Colores.TextoTenue)

            Column(
                Modifier.fillMaxWidth().background(Colores.Superficie, forma).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    parte.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.focusRequester(focoTitulo).focusable().semantics { heading() },
                )
                parte.parrafos.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonGrande(
                    if (escuchar) "Dejar de escuchar" else "Escuchar en voz alta",
                    onClick = {
                        escuchar = !escuchar
                        if (escuchar) voz.decir("Parte ${lector.indice + 1} de $total. " + parte.textoParaVoz) else voz.callar()
                    },
                    habilitado = voz.disponible,
                )
                BotonGrande("Anterior", onClick = { if (lector.anterior()) mostrar(true) }, habilitado = lector.indice > 0)
                BotonGrande("Repetir", onClick = { mostrar(true) })
                BotonGrande(
                    if (lector.esUltima) "Ir a la decisión" else "Siguiente",
                    onClick = { if (lector.siguiente()) mostrar(true) else voz.callar() },
                    principal = !lector.esUltima,
                )
            }

            Text("Tu decisión", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Row(
                Modifier
                    .toggleable(value = mayor, role = Role.Checkbox, onValueChange = { mayor = it; lector.mayorDeEdad = it })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = mayor,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = Colores.Acento, uncheckedColor = Colores.TextoTenue),
                )
                Text("Declaro que tengo 18 años o más.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
            }
            requisito?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = Colores.Aviso, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonGrande(
                    "Aceptar",
                    principal = true,
                    color = Colores.Exito,
                    onClick = {
                        val falta = lector.faltaParaAceptar()
                        requisito = falta
                        if (falta != null) {
                            voz.decir(falta)
                        } else {
                            voz.callar()
                            alAceptar(Aceptacion(Politicas.VERSION, Instant.now(), mayorDeEdad = true))
                        }
                    },
                )
                BotonGrande("Rechazar", onClick = {
                    voz.callar()
                    pausa = "No aceptaste las políticas, por eso el asistente no está disponible. Puedes leerlas de nuevo cuando quieras."
                })
                BotonGrande("Después", onClick = {
                    voz.callar()
                    pausa = "Dejaste las políticas para después. El asistente se activa cuando las aceptes."
                })
            }
        }
    }
}

// ------------------------------------------------------------------------------------ Asistente

@Composable
fun PantallaAsistente(controlador: ControladorAsistente, alAbrirAjustes: () -> Unit) {
    var pregunta by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val desplazamiento = rememberScrollState()

    fun enviar() {
        if (pregunta.isBlank()) {
            error = "Escribe una pregunta antes de enviarla."
            return
        }
        if (controlador.enviar(pregunta)) {
            pregunta = ""
            error = null
        }
    }

    LaunchedEffect(controlador.mensajes.size) { desplazamiento.animateScrollTo(desplazamiento.maxValue) }

    PantallaDividida(
        contenido = {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Asistente de CECAPI", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                controlador.estado?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Colores.Texto,
                        modifier = Modifier.fillMaxWidth().background(Colores.SuperficieElevada, forma)
                            .border(2.dp, Colores.Aviso, forma).padding(16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(desplazamiento),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (controlador.mensajes.isEmpty()) {
                        Text(
                            "Todavía no hay mensajes. Escribe tu pregunta abajo y pulsa Enter. La respuesta se lee en voz alta.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Colores.TextoTenue,
                        )
                    }
                    controlador.mensajes.forEachIndexed { i, mensaje ->
                        TarjetaMensaje(mensaje, ultima = i == controlador.mensajes.lastIndex)
                    }
                    if (controlador.esperando) {
                        Text("Esperando la respuesta…", style = MaterialTheme.typography.bodyLarge, color = Colores.TextoTenue)
                    }
                }
                OutlinedTextField(
                    value = pregunta,
                    onValueChange = { pregunta = it.take(2000); error = null },
                    label = { Text("Tu pregunta") },
                    supportingText = {
                        Text(
                            error ?: "No escribas datos personales como tu correo, teléfono, CURP, domicilio o datos de salud.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    isError = error != null,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Colores.Acento,
                        unfocusedBorderColor = Colores.Borde,
                        focusedLabelColor = Colores.Acento,
                        cursorColor = Colores.Acento,
                    ),
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { evento ->
                        // Enter envía; Mayúsculas + Enter hace salto de línea.
                        if (evento.type == KeyEventType.KeyDown && evento.key == Key.Enter && !evento.isShiftPressed) {
                            enviar()
                            true
                        } else {
                            false
                        }
                    },
                )
                BotonGrande("Enviar pregunta", onClick = ::enviar, principal = true, habilitado = !controlador.esperando)
            }
        },
        panel = {
            Column(
                Modifier.fillMaxSize().background(Colores.Superficie).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Acciones", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                BotonGrande("Repetir la respuesta (Ctrl + R)", onClick = controlador::repetir, modifier = Modifier.fillMaxWidth(), principal = true)
                BotonGrande("Callar la voz (Esc)", onClick = controlador::callar, modifier = Modifier.fillMaxWidth())
                BotonGrande("Nueva conversación", onClick = controlador::nuevaConversacion, modifier = Modifier.fillMaxWidth())
                BotonGrande("Ajustes", onClick = alAbrirAjustes, modifier = Modifier.fillMaxWidth())
                Text(
                    "Usa Tab para moverte entre botones y Enter para pulsarlos. El botón donde estás se ilumina.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.TextoTenue,
                )
            }
        },
    )
}

@Composable
private fun TarjetaMensaje(mensaje: Mensaje, ultima: Boolean) {
    val (etiqueta, color) = when (mensaje.quien) {
        Quien.PERSONA -> "Tú" to Colores.Borde
        Quien.ASISTENTE -> "Asistente" to Colores.Acento
        Quien.AVISO -> "Aviso" to Colores.Error
    }
    Column(
        Modifier.fillMaxWidth()
            .background(if (mensaje.quien == Quien.PERSONA) Colores.Fondo else Colores.Superficie, forma)
            .border(2.dp, color, forma)
            .padding(16.dp)
            .then(if (ultima && mensaje.quien != Quien.PERSONA) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
    ) {
        Text("$etiqueta:", style = MaterialTheme.typography.titleLarge, color = if (mensaje.quien == Quien.AVISO) Colores.Error else Colores.Texto)
        Text(mensaje.texto, style = MaterialTheme.typography.bodyLarge)
    }
}

// ------------------------------------------------------------------------------------ Ajustes

@Composable
fun PantallaAjustes(
    escala: Float,
    vozActiva: Boolean,
    velocidad: Int,
    vozDisponible: Boolean,
    alCambiarEscala: (Float) -> Unit,
    alCambiarVoz: (Boolean) -> Unit,
    alCambiarVelocidad: (Int) -> Unit,
    alProbarVoz: () -> Unit,
    alVolver: () -> Unit,
) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 900.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })

            Text("Tamaño del texto: ${(escala * 100).toInt()} por ciento", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonGrande("Más chico", onClick = { alCambiarEscala(escala - 0.25f) }, habilitado = escala > Preferencias.ESCALA_MINIMA)
                BotonGrande("Más grande", onClick = { alCambiarEscala(escala + 0.25f) }, habilitado = escala < Preferencias.ESCALA_MAXIMA)
            }

            Text("Voz: " + if (!vozDisponible) "no disponible en este sistema" else if (vozActiva) "encendida" else "apagada", style = MaterialTheme.typography.titleLarge)
            if (vozDisponible) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonGrande(if (vozActiva) "Apagar la voz" else "Encender la voz", onClick = { alCambiarVoz(!vozActiva) })
                    BotonGrande("Probar la voz", onClick = alProbarVoz)
                }
                Text("Velocidad de la voz: $velocidad (de -5 a 5)", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonGrande("Más lenta", onClick = { alCambiarVelocidad(velocidad - 1) }, habilitado = velocidad > -5)
                    BotonGrande("Más rápida", onClick = { alCambiarVelocidad(velocidad + 1) }, habilitado = velocidad < 5)
                }
            } else {
                Text(
                    "La voz usa el sintetizador de Windows. En este sistema no está, pero la app funciona igual con un lector de pantalla.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colores.TextoTenue,
                )
            }

            Text("Versión ${ConfiguracionGenerada.VERSION}", style = MaterialTheme.typography.bodyMedium, color = Colores.TextoTenue)
            BotonGrande("Volver al asistente", onClick = alVolver, principal = true)
        }
    }
}
