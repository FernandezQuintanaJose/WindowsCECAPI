package mx.cecapi.escritorio

import java.util.concurrent.TimeUnit

/** Lo que la app dice en voz alta. */
interface Voz {
    val disponible: Boolean
    fun decir(texto: String)
    fun callar()
    fun vocesDisponibles(): List<String> = emptyList()
}

/**
 * Voz de Windows (System.Speech), lanzada con PowerShell, que viene en Windows 10 y 11.
 * Prefiere una voz de español de México y, si no hay, cualquiera en español. El texto se pasa por
 * una variable de entorno, nunca dentro del comando, para que ningún texto pueda ejecutarse.
 * Cada frase nueva interrumpe la anterior.
 */
class VozDeWindows(private val preferencias: Preferencias) : Voz {
    override val disponible = true
    private var proceso: Process? = null

    override fun vocesDisponibles(): List<String> = try {
        val p = ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
            "Add-Type -AssemblyName System.Speech; " +
                "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "try { \$s.GetInstalledVoices() | Where-Object { \$_.Enabled } | ForEach-Object { \$_.VoiceInfo.Name } } finally { \$s.Dispose() }"
        ).redirectErrorStream(true).start()
        val salida = p.inputStream.bufferedReader().use { it.readText() }
        if (p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0)
            salida.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        else {
            p.destroyForcibly()
            emptyList()
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    override fun decir(texto: String) {
        callar()
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        proceso = try {
            ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", java.util.Base64.getEncoder().encodeToString(GUION.toByteArray(Charsets.UTF_16LE)))
                .apply {
                    environment()["CECAPI_TEXTO"] = limpio
                    environment()["CECAPI_VELOCIDAD"] = preferencias.velocidadVoz.toString()
                    environment()["CECAPI_VOLUMEN"] = preferencias.volumenVoz.toString()
                    environment()["CECAPI_TONO"] = preferencias.tonoVoz.toString()
                    environment()["CECAPI_VOZ"] = preferencias.nombreVoz
                    redirectErrorStream(true)
                    redirectOutput(ProcessBuilder.Redirect.DISCARD)
                }.start()
        } catch (_: Exception) { null }
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
            try {
                ${'$'}voces = @(${'$'}s.GetInstalledVoices() | Where-Object { ${'$'}_.Enabled })
                ${'$'}seleccionada = ${'$'}voces | Where-Object { ${'$'}_.VoiceInfo.Name -eq ${'$'}env:CECAPI_VOZ } | Select-Object -First 1
                if (-not ${'$'}seleccionada) {
                    ${'$'}seleccionada = ${'$'}voces | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -eq 'es-MX' } | Select-Object -First 1
                }
                if (-not ${'$'}seleccionada) {
                    ${'$'}seleccionada = ${'$'}voces | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -like 'es*' } | Select-Object -First 1
                }
                if (${'$'}seleccionada) { ${'$'}s.SelectVoice(${'$'}seleccionada.VoiceInfo.Name) }
                ${'$'}s.Rate = [Math]::Max(-10, [Math]::Min(10, [int]${'$'}env:CECAPI_VELOCIDAD))
                ${'$'}solicitado = [Math]::Max(0, [Math]::Min(200, [int]${'$'}env:CECAPI_VOLUMEN))
                ${'$'}s.Volume = [Math]::Min(100, ${'$'}solicitado)
                ${'$'}tono = [Math]::Max(-50, [Math]::Min(50, [int]${'$'}env:CECAPI_TONO))

                ${'$'}hablar = {
                    if (${'$'}tono -eq 0) {
                        ${'$'}s.Speak(${'$'}env:CECAPI_TEXTO)
                    } else {
                        ${'$'}idioma = ${'$'}s.Voice.Culture.Name
                        ${'$'}texto = [System.Security.SecurityElement]::Escape(${'$'}env:CECAPI_TEXTO)
                        ${'$'}signo = if (${'$'}tono -gt 0) { '+' } else { '' }
                        ${'$'}ssml = '<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="' + ${'$'}idioma + '"><prosody pitch="' + ${'$'}signo + ${'$'}tono + '%">' + ${'$'}texto + '</prosody></speak>'
                        try { ${'$'}s.SpeakSsml(${'$'}ssml) } catch { ${'$'}s.Speak(${'$'}env:CECAPI_TEXTO) }
                    }
                }

                if (${'$'}solicitado -le 100) {
                    & ${'$'}hablar
                } else {
                    # System.Speech se limita al 100 %. Renderizar PCM y multiplicar amplitud.
                    # El sistema de amplificación puede fallar según el formato WAV de la voz.
                    # En ese caso, no perder la voz: volver al sintetizador nativo al 100 %.
                    ${'$'}wavStream = New-Object System.IO.MemoryStream
                    try {
                        ${'$'}s.SetOutputToWaveStream(${'$'}wavStream)
                        & ${'$'}hablar
                        ${'$'}s.SetOutputToNull()
                        ${'$'}bytes = ${'$'}wavStream.ToArray()
                        Add-Type -TypeDefinition '
using System;
using System.Text;
public static class CecapiAudioGain {
  public static bool Amplify(byte[] wav, double factor) {
    if (wav == null || wav.Length < 44 || Encoding.ASCII.GetString(wav,0,4) != "RIFF" || Encoding.ASCII.GetString(wav,8,4) != "WAVE") return false;
    int offset=12; bool formatOk=false; int dataStart=-1, dataLength=0;
    while(offset+8 <= wav.Length) {
      int length=BitConverter.ToInt32(wav,offset+4);
      if(length<0 || length > wav.Length-offset-8) return false;
      string id=Encoding.ASCII.GetString(wav,offset,4);
      if(id=="fmt " && length>=16) {
        int format=BitConverter.ToInt16(wav,offset+8);
        int bits=BitConverter.ToInt16(wav,offset+22);
        formatOk=(format==1 && bits==16);
      }
      if(id=="data") { dataStart=offset+8; dataLength=length; }
      int advance=8+length+(length%2);
      if(advance<=0 || offset > wav.Length-advance) break;
      offset+=advance;
    }
    if(!formatOk || dataStart<0 || (dataLength%2)!=0) return false;
    for(int i=dataStart;i<dataStart+dataLength;i+=2) {
      short sample=(short)(wav[i] | wav[i+1]<<8);
      double scaled=sample*factor;
      int value=scaled > 32767 ? 32767 : scaled < -32768 ? -32768 : (int)Math.Round(scaled);
      wav[i]=(byte)(value&255);
      wav[i+1]=(byte)((value>>8)&255);
    }
    return true;
  }
}
'
                        if ([CecapiAudioGain]::Amplify(${'$'}bytes, ${'$'}solicitado / 100.0)) {
                            ${'$'}memoria = [System.IO.MemoryStream]::new(${'$'}bytes)
                            try {
                                ${'$'}memoria.Position = 0
                                ${'$'}player = [System.Media.SoundPlayer]::new(${'$'}memoria)
                                ${'$'}player.Load()
                                ${'$'}player.PlaySync()
                            } finally { ${'$'}memoria.Dispose() }
                        } else {
                            # Formato no compatible: reproducir sin amplificación.
                            ${'$'}s.SetOutputToDefaultAudioDevice()
                            & ${'$'}hablar
                        }
                    } catch {
                        # Registrar el error real para diagnosticar el WAV sin ocultarlo.
                        try {
                            ${'$'}registro = Join-Path ${'$'}env:APPDATA 'CECAPI\voz-errores.log'
                            Add-Content -LiteralPath ${'$'}registro -Value ([DateTime]::Now.ToString('s') + ' : ' + ${'$'}_.Exception.Message) -Encoding UTF8
                        } catch { }
                        # La amplificación falló: recuperar voz nativa sin elevar amplitud.
                        ${'$'}s.SetOutputToDefaultAudioDevice()
                        ${'$'}s.Volume = 100
                        & ${'$'}hablar
                    } finally {
                        ${'$'}wavStream.Dispose()
                    }
                }
            } finally { ${'$'}s.Dispose() }
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
                "-s", (145 + velocidad().coerceIn(-10, 10) * 10).toString(),
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
            VozDeWindows(preferencias)
        java.io.File("/usr/bin/espeak-ng").exists() ->
            VozDeLinux { preferencias.velocidadVoz }
        else -> SinVoz
    }
