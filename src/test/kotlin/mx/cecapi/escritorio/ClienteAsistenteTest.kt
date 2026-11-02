package mx.cecapi.escritorio

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/** El cliente contra un servidor local que imita al de Render (mismas rutas y errores de FastAPI). */
class ClienteAsistenteTest {
    private lateinit var servidor: HttpServer
    private val recibidas = mutableListOf<Pair<String, String?>>() // cuerpo, X-Clave-App
    private var codigo = 200
    private var respuesta = """{"respuesta":"Hola.","proveedor":"groq","hayMas":false,"conversacionId":"c-1"}"""

    @BeforeTest
    fun arrancar() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/preguntar") { intercambio ->
            recibidas += intercambio.requestBody.readBytes().toString(Charsets.UTF_8) to intercambio.requestHeaders.getFirst("X-Clave-App")
            val bytes = respuesta.toByteArray()
            intercambio.responseHeaders.add("Content-Type", "application/json")
            intercambio.sendResponseHeaders(codigo, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.createContext("/") { intercambio ->
            val bytes = """{"status":"CECAPI backend activo"}""".toByteArray()
            intercambio.sendResponseHeaders(200, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.start()
    }

    @AfterTest
    fun detener() = servidor.stop(0)

    private fun cliente(clave: String = "secreta") =
        ClienteAsistente("http://127.0.0.1:${servidor.address.port}", clave, Duration.ofSeconds(5), pausaReintento = 10)

    @Test
    fun `responde, manda la clave y sigue la misma conversacion`() = runBlocking {
        val c = cliente()
        val primera = c.preguntar("¿Quién fue Louis Braille?", emptyList())
        assertEquals(Resultado.Respuesta("Hola.", false, "groq"), primera)
        assertEquals("secreta", recibidas[0].second)
        assertTrue("conversacionId" !in recibidas[0].first, "la primera pregunta no lleva conversación")

        c.preguntar("¿Y cómo se consigue uno?", listOf("Persona: hola", "Asistente: Hola."))
        assertTrue("\"conversacionId\":\"c-1\"" in recibidas[1].first, "la segunda sigue la conversación c-1")
        assertTrue("Persona: hola" in recibidas[1].first, "manda el contexto")

        c.nuevaConversacion()
        assertNull(c.conversacionId)
    }

    @Test
    fun `sin clave no manda el encabezado`() = runBlocking {
        cliente(clave = "").preguntar("Hola", emptyList())
        assertNull(recibidas[0].second)
    }

    @Test
    fun `acepta la URL anterior que termina en preguntar`() = runBlocking {
        val c = ClienteAsistente("http://127.0.0.1:${servidor.address.port}/preguntar/", "", Duration.ofSeconds(5))
        assertEquals(Resultado.Respuesta("Hola.", false, "groq"), c.preguntar("Hola", emptyList()))
    }

    @Test
    fun `cada error del servidor se dice con su frase`() = runBlocking {
        val casos = mapOf(
            401 to ("""{"detail":"La app no tiene permiso para usar el asistente."}""" to Mensajes.NO_AUTORIZADA),
            429 to ("""{"detail":"Hiciste muchas preguntas seguidas."}""" to Mensajes.DEMASIADAS),
            503 to ("""{"detail":"El asistente no está disponible en este momento."}""" to Mensajes.NO_DISPONIBLE),
            400 to ("""{"detail":"La foto es demasiado grande."}""" to "La foto es demasiado grande."),
            422 to ("""{"detail":[{"loc":["body","pregunta"],"msg":"too long"}]}""" to Mensajes.PREGUNTA_INVALIDA),
        )
        for ((estado, par) in casos) {
            codigo = estado
            respuesta = par.first
            assertEquals(Resultado.Falla(par.second), cliente().preguntar("Hola", emptyList()), "código $estado")
        }
    }

    @Test
    fun `un 400 sin frase usa el mensaje general`() = runBlocking {
        codigo = 400
        respuesta = "{}"
        assertEquals(Resultado.Falla(Mensajes.RECHAZADA), cliente().preguntar("Hola", emptyList()))
    }

    @Test
    fun `sin servidor reintenta una vez y avisa que no hay conexion`() = runBlocking {
        val puerto = servidor.address.port
        servidor.stop(0)
        val c = ClienteAsistente("http://127.0.0.1:$puerto", "", Duration.ofSeconds(2), pausaReintento = 10)
        assertEquals(Resultado.Falla(Mensajes.SIN_CONEXION), c.preguntar("Hola", emptyList()))
    }

    @Test
    fun `sin servidor configurado lo dice sin intentar`() = runBlocking {
        val c = ClienteAsistente("", "")
        assertEquals(false, c.configurado)
        assertEquals(Resultado.Falla(Mensajes.SIN_SERVIDOR), c.preguntar("Hola", emptyList()))
        assertEquals(false, c.despertar())
    }

    @Test
    fun `despertar llama a la raiz`() = runBlocking {
        assertTrue(cliente().despertar())
    }
}
