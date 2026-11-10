package mx.cecapi.escritorio

import java.util.concurrent.TimeUnit

/** Lo que la app dice en voz alta. */
interface Voz {
    val disponible: Boolean
    fun decir(texto: String)
    fun callar()
}

/**
 * Voz de Windows (System.Speech), lanzada con PowerShell, que viene en Windows 10 y 11.
 * Prefiere una voz de español de México y, si no hay, cualquiera en español. El texto se pasa por
 * una variable de entorno, nunca dentro del comando, para que ningún texto pueda ejecutarse.
 * Cada frase nueva interrumpe la anterior.
 */
class VozDeWindows(private val velocidad: () -> Int) : Voz {
    override val disponible = true
    private var proceso: Process? = null

    @Synchronized
    override fun decir(texto: String) {
        callar()
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        proceso = try {
            ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", GUION)
                .apply {
                    environment()["CECAPI_TEXTO"] = limpio
                    environment()["CECAPI_VELOCIDAD"] = velocidad().coerceIn(-5, 5).toString()
                    redirectErrorStream(true)
                    redirectOutput(ProcessBuilder.Redirect.DISCARD)
                }
                .start()
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    override fun callar() {
        proceso?.let { p ->
            p.descendants().forEach { it.destroyForcibly() }
            p.destroyForcibly()
            p.waitFor(1, TimeUnit.SECONDS)
        }
        proceso = null
    }

    private companion object {
        val GUION = """
            Add-Type -AssemblyName System.Speech
            ${'$'}s = New-Object System.Speech.Synthesis.SpeechSynthesizer
            ${'$'}voces = ${'$'}s.GetInstalledVoices() | Where-Object { ${'$'}_.Enabled -and ${'$'}_.VoiceInfo.Culture.Name -like 'es*' }
            ${'$'}voz = ${'$'}voces | Sort-Object { ${'$'}_.VoiceInfo.Culture.Name -ne 'es-MX' } | Select-Object -First 1
            if (${'$'}voz) { ${'$'}s.SelectVoice(${'$'}voz.VoiceInfo.Name) }
            ${'$'}s.Rate = [int]${'$'}env:CECAPI_VELOCIDAD
            ${'$'}s.Speak(${'$'}env:CECAPI_TEXTO)
        """.trimIndent()
    }
}


/** Síntesis de voz mediante eSpeak NG para desarrollo en Linux. */
class VozDeLinux(private val velocidad: () -> Int) : Voz {
    override val disponible = true
    private var proceso: Process? = null

    @Synchronized
    override fun decir(texto: String) {
        callar()
        if (texto.isBlank()) return

        proceso = try {
            ProcessBuilder(
                "espeak-ng",
                "-v", "es",
                "-a", "200",
                "-s", (145 + velocidad().coerceIn(-5, 5) * 10).toString(),
                "-p", "45",
                texto
            ).start()
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    override fun callar() {
        proceso?.destroyForcibly()
        proceso = null
    }
}

object SinVoz : Voz {
    override val disponible = false
    override fun decir(texto: String) = Unit
    override fun callar() = Unit
}

fun crearVoz(preferencias: Preferencias): Voz =
    when {
        System.getProperty("os.name").orEmpty().startsWith("Windows") ->
            VozDeWindows { preferencias.velocidadVoz }
        java.io.File("/usr/bin/espeak-ng").exists() ->
            VozDeLinux { preferencias.velocidadVoz }
        else -> SinVoz
    }
