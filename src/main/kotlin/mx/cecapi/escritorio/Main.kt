package mx.cecapi.escritorio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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

/** Despacho de teclas desde la ventana, incluso cuando el foco está en otro control. */
class AtajosCecapi {
    var pantalla: ((Key) -> Boolean)? = null
    var asistente: ((Key) -> Boolean)? = null
    fun ejecutar(tecla: Key): Boolean = pantalla?.invoke(tecla) == true || asistente?.invoke(tecla) == true
}

fun main() = application {
    val preferencias = remember { Preferencias() }
    val voz = remember { crearVoz(preferencias) }
    val alcance = rememberCoroutineScope()
    val microfono = remember { MicrofonoWindows() }
    val atajos = remember { AtajosCecapi() }
    val prueba = remember { if (System.getenv("CECAPI_MODO_PRUEBA") == "1") ServidorPrueba() else null }
    DisposableEffect(Unit) { onDispose { microfono.detener(); prueba?.close() } }
    val controlador = remember {
        ControladorAsistente(
            cliente = ClienteAsistente(prueba?.url ?: ConfiguracionGenerada.SERVIDOR, if (prueba != null) "" else ConfiguracionGenerada.CLAVE_APP),
            voz = voz,
            alcance = alcance,
            vozActiva = { preferencias.vozActiva },
        )
    }
    Window(
        onCloseRequest = {
            microfono.detener()
            voz.callar()
            exitApplication()
        },
        title = "CECAPI · Asistente",
        state = rememberWindowState(size = DpSize(1180.dp, 820.dp)),
        onPreviewKeyEvent = { evento ->
            // Atajos de toda la ventana: Esc calla la voz; Ctrl + R repite la última respuesta.
            when {
                evento.type != KeyEventType.KeyDown -> false
                evento.key == Key.Escape -> { microfono.detener(); controlador.callar(); true }
                evento.isCtrlPressed && evento.key == Key.R -> { microfono.detener(); controlador.repetir(); true }
                else -> atajos.ejecutar(evento.key)
            }
        },
    ) {
        App(preferencias, voz, controlador, microfono, prueba != null, atajos)
    }
}

@Composable
fun App(preferencias: Preferencias, voz: Voz, controlador: ControladorAsistente, microfono: MicrofonoWindows? = null, modoPrueba: Boolean = false, atajos: AtajosCecapi? = null) {
    var escala by remember { mutableStateOf(preferencias.escalaTexto) }
    var vozActiva by remember { mutableStateOf(preferencias.vozActiva) }
    var velocidad by remember { mutableIntStateOf(preferencias.velocidadVoz) }
    var volumen by remember { mutableIntStateOf(preferencias.volumenVoz) }
    var tono by remember { mutableIntStateOf(preferencias.tonoVoz) }
    var nombreVoz by remember { mutableStateOf(preferencias.nombreVoz) }
    val vocesDisponibles = remember(voz) { voz.vocesDisponibles() }
    var aceptado by remember { mutableStateOf(preferencias.aceptoVersion(Politicas.VERSION)) }
    var enAjustes by remember { mutableStateOf(false) }

    fun alternarAjustes() {
        microfono?.detener()
        enAjustes = !enAjustes
        controlador.confirmar(if (enAjustes) "Ajustes. Pulsa F5 para volver al asistente." else "Asistente. Pulsa espacio con el foco en el micrófono o F2 para dictar; F3 para dar un comando.")
    }
    fun cambiarLectura(activa: Boolean) {
        preferencias.vozActiva = activa
        vozActiva = activa
        microfono?.detener()
        voz.callar()
        controlador.confirmar(if (activa) "Lectura automática encendida." else "Lectura automática apagada. Las acciones solicitadas todavía tienen aviso de voz.")
    }
    SideEffect {
        atajos?.pantalla = { tecla ->
            when {
                tecla == Key.F1 -> {
                    microfono?.detener()
                    controlador.confirmar(if (!aceptado) "Políticas de uso. Usa Tab para recorrer los controles y Enter o espacio para activarlos." else
                        "Espacio activa o detiene el micrófono cuando tiene el foco. F2 dictar pregunta. F3 comando de voz. F4 leer pregunta. F5 abrir o cerrar ajustes. F6 activar o desactivar lectura automática. F7 enviar pregunta. Control R repetir respuesta. Escape callar o cancelar. Tab recorrer controles. Lectura automática " + if (vozActiva) "encendida." else "apagada.")
                    true
                }
                !aceptado -> false
                tecla == Key.F5 -> { alternarAjustes(); true }
                tecla == Key.F6 -> { cambiarLectura(!vozActiva); true }
                enAjustes && tecla in listOf(Key.F2, Key.F3, Key.F4, Key.F7) -> { controlador.confirmar("Estás en ajustes. Pulsa F5 para volver al asistente."); true }
                else -> false
            }
        }
    }
    DisposableEffect(atajos) { onDispose { atajos?.pantalla = null } }
    LaunchedEffect(Unit) { controlador.despertar() }
    LaunchedEffect(aceptado) {
        if (aceptado && preferencias.vozActiva) controlador.confirmar("Asistente CECAPI. Pulsa espacio con el foco en el micrófono para dictar. F1 para escuchar los atajos.")
    }

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
                volumen = volumen,
                tono = tono,
                nombreVoz = nombreVoz,
                vocesDisponibles = vocesDisponibles,
                vozDisponible = voz.disponible,
                alCambiarEscala = { preferencias.escalaTexto = it; escala = preferencias.escalaTexto },
                alCambiarVoz = ::cambiarLectura,
                alCambiarVelocidad = { preferencias.velocidadVoz = it; velocidad = preferencias.velocidadVoz },
                alCambiarVolumen = { preferencias.volumenVoz = it; volumen = preferencias.volumenVoz },
                alCambiarTono = { preferencias.tonoVoz = it; tono = preferencias.tonoVoz },
                alCambiarNombreVoz = { preferencias.nombreVoz = it; nombreVoz = preferencias.nombreVoz },
                alProbarVoz = { voz.decir("Esta es una prueba de la voz seleccionada, con los nuevos ajustes.") },
                alVolver = ::alternarAjustes,
            )
            else -> PantallaAsistente(controlador, microfono = microfono, modoPrueba = modoPrueba, atajos = atajos, lecturaAutomatica = vozActiva,
                alComando = { comando ->
                    when (comando) {
                        ComandoVoz.CALLAR -> { voz.callar(); "" }
                        ComandoVoz.REPETIR -> { controlador.repetir(); "" }
                        ComandoVoz.SUBIR_VOLUMEN, ComandoVoz.BAJAR_VOLUMEN -> {
                            preferencias.volumenVoz += if (comando == ComandoVoz.SUBIR_VOLUMEN) 10 else -10
                            volumen = preferencias.volumenVoz; "Volumen al $volumen por ciento."
                        }
                        ComandoVoz.RAPIDA, ComandoVoz.LENTA -> {
                            preferencias.velocidadVoz += if (comando == ComandoVoz.RAPIDA) 1 else -1
                            velocidad = preferencias.velocidadVoz; "Velocidad de lectura ajustada a $velocidad."
                        }
                        ComandoVoz.AGUDA, ComandoVoz.GRAVE -> {
                            preferencias.tonoVoz += if (comando == ComandoVoz.AGUDA) 10 else -10
                            tono = preferencias.tonoVoz; "Tono de voz ajustado a $tono."
                        }
                        ComandoVoz.AJUSTES -> { enAjustes = true; "Abriendo ajustes." }
                        ComandoVoz.ENVIAR -> ""
                    }.also { if (it.isNotBlank()) controlador.confirmar(it) }
                }, alAbrirAjustes = ::alternarAjustes)
        }
    }
}
