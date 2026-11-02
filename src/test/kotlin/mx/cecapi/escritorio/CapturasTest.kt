package mx.cecapi.escritorio

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.Composable
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/**
 * Dibuja cada pantalla fuera de la ventana y la guarda como PNG en build/capturas, para revisarlas sin
 * Windows. Solo corre con `./gradlew test -Pcapturas`; en una prueba normal no hace nada.
 */
class CapturasTest {
    private val activo = System.getProperty("capturas") == "si"
    private val carpeta = File(System.getProperty("carpetaCapturas") ?: "build/capturas")

    private fun guardar(nombre: String, ancho: Int, alto: Int, contenido: @Composable () -> Unit) {
        carpeta.mkdirs()
        val escena = ImageComposeScene(ancho, alto, Density(1f), content = contenido)
        try {
            escena.render() // el primer cuadro acomoda todo; el segundo ya trae lo que dependía de él
            val imagen = escena.render()
            val datos = imagen.encodeToData(EncodedImageFormat.PNG) ?: error("No se pudo codificar $nombre")
            File(carpeta, "$nombre.png").writeBytes(datos.bytes)
        } finally {
            escena.close()
        }
    }

    private fun controladorDeMuestra(): ControladorAsistente {
        val c = ControladorAsistente(ClienteAsistente("", ""), SinVoz, CoroutineScope(Dispatchers.Unconfined), { true })
        c.mensajes += Mensaje(Quien.PERSONA, "¿Quién fue Louis Braille?")
        c.mensajes += Mensaje(Quien.ASISTENTE, "Louis Braille fue un francés que inventó el sistema de lectura con puntos en relieve.")
        c.mensajes += Mensaje(Quien.AVISO, Mensajes.DEMASIADAS)
        return c
    }

    @Test
    fun capturas() {
        if (!activo) return
        val prefs = Preferencias(Files.createTempDirectory("capturas").resolve("p.properties"))
        guardar("politicas", 1180, 820) { TemaCecapi(1f) { PantallaPoliticas(SinVoz) {} } }
        guardar("asistente-ancho", 1180, 820) { TemaCecapi(1f) { PantallaAsistente(controladorDeMuestra()) {} } }
        guardar("asistente-angosto", 700, 900) { TemaCecapi(1f) { PantallaAsistente(controladorDeMuestra()) {} } }
        guardar("asistente-texto-grande", 1180, 820) { TemaCecapi(1.5f) { PantallaAsistente(controladorDeMuestra()) {} } }
        guardar("ajustes", 1180, 820) {
            TemaCecapi(1f) { PantallaAjustes(1.25f, true, 0, true, {}, {}, {}, {}, {}) }
        }
        guardar("app-sin-aceptar", 1180, 820) { App(prefs, SinVoz, controladorDeMuestra()) }
    }
}
