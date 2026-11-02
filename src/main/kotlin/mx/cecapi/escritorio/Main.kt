package mx.cecapi.escritorio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val preferencias = remember { Preferencias() }
    val voz = remember { crearVoz(preferencias) }
    val alcance = rememberCoroutineScope()
    val controlador = remember {
        ControladorAsistente(
            cliente = ClienteAsistente(ConfiguracionGenerada.SERVIDOR, ConfiguracionGenerada.CLAVE_APP),
            voz = voz,
            alcance = alcance,
            vozActiva = { preferencias.vozActiva },
        )
    }
    Window(
        onCloseRequest = {
            voz.callar()
            exitApplication()
        },
        title = "CECAPI · Asistente",
        state = rememberWindowState(size = DpSize(1180.dp, 820.dp)),
        onPreviewKeyEvent = { evento ->
            // Atajos de toda la ventana: Esc calla la voz; Ctrl + R repite la última respuesta.
            when {
                evento.type != KeyEventType.KeyDown -> false
                evento.key == Key.Escape -> { controlador.callar(); true }
                evento.isCtrlPressed && evento.key == Key.R -> { controlador.repetir(); true }
                else -> false
            }
        },
    ) {
        App(preferencias, voz, controlador)
    }
}

@Composable
fun App(preferencias: Preferencias, voz: Voz, controlador: ControladorAsistente) {
    var escala by remember { mutableStateOf(preferencias.escalaTexto) }
    var vozActiva by remember { mutableStateOf(preferencias.vozActiva) }
    var velocidad by remember { mutableIntStateOf(preferencias.velocidadVoz) }
    var aceptado by remember { mutableStateOf(preferencias.aceptoVersion(Politicas.VERSION)) }
    var enAjustes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { controlador.despertar() }

    TemaCecapi(escala) {
        when {
            !aceptado -> PantallaPoliticas(voz) { aceptacion ->
                preferencias.aceptacion = aceptacion
                aceptado = true
            }
            enAjustes -> PantallaAjustes(
                escala = escala,
                vozActiva = vozActiva,
                velocidad = velocidad,
                vozDisponible = voz.disponible,
                alCambiarEscala = { preferencias.escalaTexto = it; escala = preferencias.escalaTexto },
                alCambiarVoz = {
                    preferencias.vozActiva = it
                    vozActiva = it
                    if (!it) voz.callar()
                },
                alCambiarVelocidad = { preferencias.velocidadVoz = it; velocidad = preferencias.velocidadVoz },
                alProbarVoz = { voz.decir("Así sueno con esta velocidad.") },
                alVolver = { enAjustes = false },
            )
            else -> PantallaAsistente(controlador, alAbrirAjustes = { enAjustes = true })
        }
    }
}
