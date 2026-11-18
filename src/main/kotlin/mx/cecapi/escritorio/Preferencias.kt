package mx.cecapi.escritorio

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.Properties

/** Constancia de que se aceptaron las políticas: qué versión, cuándo y la declaración de 18 años o más. */
data class Aceptacion(val version: String, val fecha: Instant, val mayorDeEdad: Boolean)

/**
 * Preferencias de esta computadora, en un archivo de texto en la carpeta del usuario de Windows
 * (`%APPDATA%\CECAPI\preferencias.properties`). No guarda preguntas ni respuestas: solo ajustes
 * y la constancia de aceptación de las políticas.
 */
class Preferencias(private val archivo: Path = carpetaDeDatos().resolve("preferencias.properties")) {
    private val datos = Properties()

    init {
        if (Files.exists(archivo)) {
            try {
                Files.newInputStream(archivo).use { datos.load(it) }
            } catch (e: Exception) {
                // Un archivo dañado no debe impedir abrir la app: se empieza de cero.
                datos.clear()
            }
        }
    }

    var escalaTexto: Float
        get() = datos.getProperty("escalaTexto")?.toFloatOrNull()?.coerceIn(ESCALA_MINIMA, ESCALA_MAXIMA) ?: 1f
        set(valor) = guardar("escalaTexto", valor.coerceIn(ESCALA_MINIMA, ESCALA_MAXIMA).toString())

    var vozActiva: Boolean
        get() = datos.getProperty("vozActiva")?.toBooleanStrictOrNull() ?: true
        set(valor) = guardar("vozActiva", valor.toString())

    /** Velocidad System.Speech de -10 (lenta) a +10 (rapida). */
    var velocidadVoz: Int
        get() = datos.getProperty("velocidadVoz")?.toIntOrNull()?.coerceIn(-10, 10) ?: 0
        set(valor) = guardar("velocidadVoz", valor.coerceIn(-10, 10).toString())

    var volumenVoz: Int
        get() = datos.getProperty("volumenVoz")?.toIntOrNull()?.coerceIn(0, 200) ?: 100
        set(valor) = guardar("volumenVoz", valor.coerceIn(0, 200).toString())

    /** Tono SSML orientativo: de -50 a +50 por ciento. */
    var tonoVoz: Int
        get() = datos.getProperty("tonoVoz")?.toIntOrNull()?.coerceIn(-50, 50) ?: 0
        set(valor) = guardar("tonoVoz", valor.coerceIn(-50, 50).toString())

    /** Nombre exacto de una voz instalada; vacio = idioma espanol automatico. */
    var nombreVoz: String
        get() = datos.getProperty("nombreVoz").orEmpty()
        set(valor) = guardar("nombreVoz", valor.take(200))

    var aceptacion: Aceptacion?
        get() {
            val version = datos.getProperty("aceptacion.version") ?: return null
            val fecha = datos.getProperty("aceptacion.fecha")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
            val mayor = datos.getProperty("aceptacion.mayorDeEdad")?.toBooleanStrictOrNull() ?: return null
            return Aceptacion(version, fecha, mayor)
        }
        set(valor) {
            if (valor == null) {
                datos.remove("aceptacion.version")
                datos.remove("aceptacion.fecha")
                datos.remove("aceptacion.mayorDeEdad")
                escribir()
            } else {
                datos.setProperty("aceptacion.version", valor.version)
                datos.setProperty("aceptacion.fecha", valor.fecha.toString())
                datos.setProperty("aceptacion.mayorDeEdad", valor.mayorDeEdad.toString())
                escribir()
            }
        }

    /** La aceptación cuenta solo si es de la versión vigente de las políticas y declaró 18 años o más. */
    fun aceptoVersion(version: String): Boolean = aceptacion?.let { it.version == version && it.mayorDeEdad } == true

    private fun guardar(clave: String, valor: String) {
        datos.setProperty(clave, valor)
        escribir()
    }

    private fun escribir() {
        try {
            Files.createDirectories(archivo.parent)
            Files.newOutputStream(archivo).use { datos.store(it, "Preferencias de CECAPI") }
        } catch (e: Exception) {
            // Sin permiso de escritura la app sigue funcionando; solo no recordará los ajustes.
        }
    }

    companion object {
        const val ESCALA_MINIMA = 0.5f
        const val ESCALA_MAXIMA = 2f

        /** %APPDATA%\CECAPI en Windows; ~/.config/cecapi en otros sistemas (para desarrollo). */
        fun carpetaDeDatos(): Path {
            val appData = System.getenv("APPDATA")
            return if (!appData.isNullOrBlank()) {
                Paths.get(appData, "CECAPI")
            } else {
                Paths.get(System.getProperty("user.home"), ".config", "cecapi")
            }
        }
    }
}
