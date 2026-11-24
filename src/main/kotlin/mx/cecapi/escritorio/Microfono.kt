package mx.cecapi.escritorio

import java.text.Normalizer
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface CapturaVoz {
    data class Texto(val texto: String) : CapturaVoz
    data class Aviso(val mensaje: String) : CapturaVoz
}

/** Una sola frase por activación; no conserva audio ni utiliza servicios remotos. */
class MicrofonoWindows {
    val disponible = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    private var proceso: Process? = null
    private var generacion = 0L

    @Synchronized fun detener() {
        generacion++
        proceso?.destroyForcibly()
        proceso = null
    }

    suspend fun escuchar(soloComandos: Boolean = false): CapturaVoz {
        val turno = synchronized(this) {
            detener()
            generacion
        }
        return withContext(Dispatchers.IO) {
            if (!disponible) return@withContext CapturaVoz.Aviso("El micrófono de ALIA requiere Windows.")
            var activo: Process? = null
            try {
                val p = synchronized(this@MicrofonoWindows) {
                    if (turno != generacion) return@withContext CapturaVoz.Aviso("Escucha detenida.")
                    ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand",
                        Base64.getEncoder().encodeToString(GUION.toByteArray(Charsets.UTF_16LE)))
                        .apply { environment()["CECAPI_SOLO_COMANDOS"] = if (soloComandos) "1" else "0" }
                        .redirectErrorStream(true).start().also { proceso = it }
                }
                activo = p
                if (!p.waitFor(30, TimeUnit.SECONDS)) {
                    p.destroyForcibly()
                    return@withContext CapturaVoz.Aviso("Tiempo de escucha agotado. Intenta de nuevo.")
                }
                if (synchronized(this@MicrofonoWindows) { turno != generacion })
                    return@withContext CapturaVoz.Aviso("Escucha detenida.")
                val salida = p.inputStream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
                val linea = salida.lastOrNull { it.startsWith("CECAPI_OK:") }
                when {
                    linea != null -> CapturaVoz.Texto(String(Base64.getDecoder().decode(linea.substringAfter(':')), Charsets.UTF_8))
                    salida.any { it.startsWith("CECAPI_EMPTY") } -> CapturaVoz.Aviso("No se reconoció una frase clara. Intenta de nuevo.")
                    salida.any { it.startsWith("CECAPI_LANG") } -> CapturaVoz.Aviso("Instala un reconocedor de voz en español en Windows.")
                    else -> CapturaVoz.Aviso("No se pudo usar el micrófono. Revisa permisos, dispositivo e idioma de Windows.")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                CapturaVoz.Aviso("No se pudo iniciar el reconocimiento de voz.")
            } finally {
                activo?.destroyForcibly()
                synchronized(this@MicrofonoWindows) { if (proceso === activo) proceso = null }
            }
        }
    }

    private companion object {
        val GUION = """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}motor = ${'$'}null
            try {
                Add-Type -AssemblyName System.Speech
                ${'$'}info = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers() | Where-Object { ${'$'}_.Culture.Name -eq 'es-MX' } | Select-Object -First 1
                if (-not ${'$'}info) { ${'$'}info = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers() | Where-Object { ${'$'}_.Culture.TwoLetterISOLanguageName -eq 'es' } | Select-Object -First 1 }
                if (-not ${'$'}info) { Write-Output 'CECAPI_LANG'; return }
                ${'$'}motor = New-Object System.Speech.Recognition.SpeechRecognitionEngine -ArgumentList ${'$'}info.Id
                ${'$'}motor.SetInputToDefaultAudioDevice()
                ${'$'}motor.InitialSilenceTimeout = [TimeSpan]::FromSeconds(12)
                ${'$'}motor.BabbleTimeout = [TimeSpan]::FromSeconds(8)
                ${'$'}motor.EndSilenceTimeout = [TimeSpan]::FromMilliseconds(900)
                ${'$'}umbral = 0.45
                if (${'$'}env:CECAPI_SOLO_COMANDOS -eq '1') {
                    ${'$'}opciones = New-Object System.Speech.Recognition.Choices
                    ${'$'}opciones.Add([string[]]@('silencio','calla','callar la voz','repite','repite la respuesta','repetir la respuesta','sube el volumen','subir volumen','baja el volumen','bajar volumen','habla más rápido','más rápido','habla más despacio','habla más lento','más lento','voz más aguda','tono más agudo','voz más grave','tono más grave','abre ajustes','abrir ajustes','ajustes','configuración','sube volumen','baja volumen','repetir','enviar pregunta','envía la pregunta','enviar'))
                    ${'$'}constructor = New-Object System.Speech.Recognition.GrammarBuilder
                    ${'$'}constructor.Culture = ${'$'}info.Culture
                    ${'$'}constructor.Append(${'$'}opciones)
                    ${'$'}gramatica = [System.Speech.Recognition.Grammar]::new(${'$'}constructor)
                    ${'$'}motor.LoadGrammar(${'$'}gramatica)
                    ${'$'}umbral = 0.60
                } else {
                    ${'$'}motor.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
                }
                # Signal only after the engine, input and grammar have been configured.
                try { [Console]::Beep(900, 180) } catch { [System.Media.SystemSounds]::Asterisk.Play(); Start-Sleep -Milliseconds 250 }
                ${'$'}r = ${'$'}motor.Recognize([TimeSpan]::FromSeconds(12))
                ${'$'}motor.SetInputToNull()
                try { [Console]::Beep(500, 130) } catch { [System.Media.SystemSounds]::Asterisk.Play() }
                if (${'$'}null -ne ${'$'}r -and ${'$'}r.Confidence -ge ${'$'}umbral -and -not [string]::IsNullOrWhiteSpace(${'$'}r.Text)) {
                    Write-Output ('CECAPI_OK:' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(${'$'}r.Text)))
                } else { Write-Output 'CECAPI_EMPTY' }
            } catch { Write-Output 'CECAPI_ERROR' }
            finally { if (${'$'}motor) { ${'$'}motor.Dispose() } }
        """.trimIndent()
    }
}

enum class ComandoVoz { CALLAR, REPETIR, SUBIR_VOLUMEN, BAJAR_VOLUMEN, RAPIDA, LENTA, AGUDA, GRAVE, AJUSTES, ENVIAR }

fun identificarComando(texto: String): ComandoVoz? {
    val normal = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]"), " ").trim().replace(Regex("\\s+"), " ")
    return when (normal) {
        "silencio", "calla", "callar la voz" -> ComandoVoz.CALLAR
        "repite", "repetir", "repite la respuesta", "repetir la respuesta" -> ComandoVoz.REPETIR
        "sube el volumen", "sube volumen", "subir volumen" -> ComandoVoz.SUBIR_VOLUMEN
        "baja el volumen", "baja volumen", "bajar volumen" -> ComandoVoz.BAJAR_VOLUMEN
        "habla mas rapido", "habla mas rapida", "mas rapido" -> ComandoVoz.RAPIDA
        "habla mas despacio", "habla mas lento", "mas lento" -> ComandoVoz.LENTA
        "voz mas aguda", "tono mas agudo" -> ComandoVoz.AGUDA
        "voz mas grave", "tono mas grave" -> ComandoVoz.GRAVE
        "abre ajustes", "abrir ajustes", "ajustes", "configuracion" -> ComandoVoz.AJUSTES
        "enviar pregunta", "envia la pregunta", "enviar" -> ComandoVoz.ENVIAR
        else -> null
    }
}
