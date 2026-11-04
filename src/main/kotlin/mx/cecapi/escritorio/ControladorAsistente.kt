package mx.cecapi.escritorio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Quien { PERSONA, ASISTENTE, AVISO }

data class Mensaje(val quien: Quien, val texto: String)

/**
 * La conversación con el asistente: manda la pregunta, avisa si el servidor está despertando y lee
 * la respuesta en voz alta. Usa la misma regla que la app Android y la web: el contexto son las
 * últimas 6 líneas ("Persona: ..." / "Asistente: ...").
 */
class ControladorAsistente(
    private val cliente: ClienteAsistente,
    private val voz: Voz,
    private val alcance: CoroutineScope,
    private val vozActiva: () -> Boolean,
    private val avisoDespertarMs: Long = 4_000,
) {
    val mensajes = mutableStateListOf<Mensaje>()
    var esperando by mutableStateOf(false)
        private set
    var estado by mutableStateOf<String?>(if (cliente.configurado) null else Mensajes.SIN_SERVIDOR)
        private set

    private var trabajo: Job? = null

    /** Al abrir la app se despierta al servidor mientras la persona lee o escribe. */
    fun despertar() {
        if (!cliente.configurado) return
        alcance.launch {
            val aviso = launch {
                delay(avisoDespertarMs)
                estado = "El asistente se está preparando. Puede tardar cerca de un minuto."
            }
            val listo = cliente.despertar()
            aviso.cancel()
            if (estado != null) estado = if (listo) "El asistente está listo." else Mensajes.SIN_CONEXION
        }
    }

    fun enviar(texto: String): Boolean {
        val pregunta = texto.trim()
        if (pregunta.isEmpty() || esperando) return false
        val contexto = mensajes.filter { it.quien != Quien.AVISO }.takeLast(6).map {
            // El servidor acepta líneas de hasta 1000 caracteres, con el prefijo incluido.
            val prefijo = if (it.quien == Quien.PERSONA) "Persona: " else "Asistente: "
            prefijo + it.texto.take(1000 - prefijo.length)
        }
        mensajes += Mensaje(Quien.PERSONA, pregunta)
        esperando = true
        voz.callar()
        trabajo = alcance.launch {
            val aviso = launch {
                delay(avisoDespertarMs)
                estado = Mensajes.DESPERTANDO
                if (vozActiva()) voz.decir(Mensajes.DESPERTANDO)
            }
            val resultado = cliente.preguntar(pregunta, contexto)
            aviso.cancel()
            if (cliente.configurado) estado = null
            when (resultado) {
                is Resultado.Respuesta -> {
                    mensajes += Mensaje(Quien.ASISTENTE, resultado.texto)
                    if (vozActiva()) voz.decir(resultado.texto)
                }
                is Resultado.Falla -> {
                    mensajes += Mensaje(Quien.AVISO, resultado.mensaje)
                    if (vozActiva()) voz.decir(resultado.mensaje)
                }
            }
            esperando = false
        }
        return true
    }

    /** Vuelve a decir la última respuesta (o el último aviso). */
    fun repetir() {
        val ultimo = mensajes.lastOrNull { it.quien != Quien.PERSONA } ?: return
        voz.decir(ultimo.texto)
    }

    fun callar() = voz.callar()

    fun nuevaConversacion() {
        trabajo?.cancel()
        esperando = false
        voz.callar()
        mensajes.clear()
        cliente.nuevaConversacion()
    }
}
