package mx.cecapi.escritorio

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import kotlinx.serialization.json.*

/** Servidor HTTP de demostración, accesible exclusivamente desde esta computadora. No es IA. */
class ServidorPrueba : AutoCloseable {
    private val servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val url: String get() = "http://127.0.0.1:${servidor.address.port}"
    init {
        servidor.createContext("/") { e ->
            try {
                val (codigo, cuerpo) = when {
                    e.requestURI.path == "/" && e.requestMethod == "GET" -> 200 to buildJsonObject { put("modo", "simulado") }
                    e.requestURI.path == "/preguntar" && e.requestMethod == "POST" -> {
                        val datos = e.requestBody.use { it.readNBytes(65537) }
                        val entrada = if (datos.size <= 65536) runCatching { Json.parseToJsonElement(String(datos, UTF_8)).jsonObject }.getOrNull() else null
                        val pregunta = (entrada?.get("pregunta") as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()
                        if (pregunta.isNullOrBlank() || pregunta.length > 2000) 422 to buildJsonObject { put("detail", "Pregunta inválida") }
                        else 200 to buildJsonObject {
                            put("respuesta", "Modo de prueba: recibí tu pregunta correctamente. Esta respuesta es simulada; no la generó una inteligencia artificial.")
                            put("proveedor", "simulado-local")
                            put("hayMas", false)
                            put("conversacionId", (entrada?.get("conversacionId") as? JsonPrimitive)?.contentOrNull ?: UUID.randomUUID().toString())
                        }
                    }
                    else -> 404 to buildJsonObject { put("detail", "Ruta no disponible") }
                }
                val bytes = cuerpo.toString().toByteArray(UTF_8)
                e.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                e.sendResponseHeaders(codigo, bytes.size.toLong())
                e.responseBody.use { it.write(bytes) }
            } finally { e.close() }
        }
        servidor.start()
    }
    override fun close() { servidor.stop(0) }
}
