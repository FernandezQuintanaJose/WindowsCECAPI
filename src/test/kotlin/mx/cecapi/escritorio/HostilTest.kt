package mx.cecapi.escritorio

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pruebas de quien no confía en nada: un servidor roto o malicioso, una URL mal escrita, un archivo de
 * preferencias manipulado y textos largos. Lo que importa es que la app nunca se quede esperando para
 * siempre ni se caiga, y que diga algo que se pueda leer en voz alta.
 */
class HostilTest {
    private lateinit var servidor: HttpServer
    private var codigo = 200
    private var cuerpo = ""
    private var demoraMs = 0L
    private var cabeceras = mapOf<String, String>()
    private val cuerpos = mutableListOf<String>()

    @BeforeTest
    fun arrancar() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/preguntar") { x ->
            cuerpos += x.requestBody.readBytes().toString(Charsets.UTF_8)
            if (demoraMs > 0) Thread.sleep(demoraMs)
            val bytes = cuerpo.toByteArray()
            cabeceras.forEach { (k, v) -> x.responseHeaders.add(k, v) }
            x.sendResponseHeaders(codigo, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) x.responseBody.use { it.write(bytes) } else x.close()
        }
        servidor.start()
    }

    @AfterTest
    fun detener() = servidor.stop(0)

    private fun url() = "http://127.0.0.1:${servidor.address.port}"
    private fun cliente() = ClienteAsistente(url(), "", Duration.ofSeconds(2), pausaReintento = 10)

    @Test
    fun `un 200 que no es JSON no tumba al cliente`() = runBlocking {
        for (malo in listOf("<html>Bienvenido al WiFi</html>", "", "null", "[]", "{}", "{\"respuesta\":null}", "{\"respuesta\":{\"a\":1}}", "\u0000\u0001\u0002", "{")) {
            cuerpo = malo
            val r = cliente().preguntar("hola", emptyList())
            assertIs<Resultado.Falla>(r, "cuerpo '${malo.take(20)}' debe ser una falla con mensaje, fue $r")
            assertTrue(r.mensaje.isNotBlank())
        }
    }

    @Test
    fun `una respuesta en blanco no cuenta como respuesta`() = runBlocking {
        cuerpo = """{"respuesta":"   ","proveedor":"groq"}"""
        val r = cliente().preguntar("hola", emptyList())
        assertTrue(r is Resultado.Falla, "una respuesta vacía se leería en silencio; debe avisar, fue $r")
    }

    @Test
    fun `un error con cuerpo basura o vacio dice una frase`() = runBlocking {
        for (c in listOf(400, 401, 404, 418, 429, 500, 502, 503)) {
            codigo = c
            for (malo in listOf("", "<h1>boom</h1>", "{\"detail\":{\"x\":1}}", "{\"detail\":\"\"}", "{\"detail\":[1,2]}")) {
                cuerpo = malo
                val r = cliente().preguntar("hola", emptyList())
                assertIs<Resultado.Falla>(r)
                assertTrue(r.mensaje.isNotBlank(), "$c con '$malo'")
            }
        }
    }

    @Test
    fun `no sigue redirecciones a otro servidor`() = runBlocking {
        val otro = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var visitado = false
        otro.createContext("/") { x -> visitado = true; x.sendResponseHeaders(200, -1); x.close() }
        otro.start()
        try {
            codigo = 302
            cabeceras = mapOf("Location" to "http://127.0.0.1:${otro.address.port}/robado")
            val r = cliente().preguntar("hola", emptyList())
            assertIs<Resultado.Falla>(r)
            assertFalse(visitado, "no debe mandar la pregunta (ni la clave) a donde lo redirija un servidor")
        } finally {
            otro.stop(0)
        }
    }

    @Test
    fun `un servidor que no contesta termina en una falla y no cuelga`() = runBlocking {
        demoraMs = 4_000
        val inicio = System.nanoTime()
        val r = withTimeoutOrNull(20_000) { cliente().preguntar("hola", emptyList()) }
        val seg = (System.nanoTime() - inicio) / 1e9
        assertIs<Resultado.Falla>(r, "debe fallar con mensaje, fue $r")
        assertTrue(seg < 12, "con espera de 2 s y 2 intentos tardó $seg s")
    }

    @Test
    fun `una direccion de servidor mal escrita no lanza excepciones`() = runBlocking {
        for (mala in listOf("http://127.0.0.1:99 99", "https://x y.onrender.com", "http://", "http:// ", "httpx://exa mple", "http://[::1", "http://a b/preguntar")) {
            val c = ClienteAsistente(mala, "", Duration.ofSeconds(1), pausaReintento = 1)
            val r = try { c.preguntar("hola", emptyList()) } catch (e: Throwable) { fail("'$mala' lanzó ${e::class.simpleName}: ${e.message}") }
            assertIs<Resultado.Falla>(r)
            assertFalse(c.despertar().also { }, "despertar con '$mala' no debe lanzar ni dar true")
        }
    }

    @Test
    fun `una clave de la app con saltos de linea no lanza excepciones`() = runBlocking {
        for (mala in listOf("clave\nmala", "clave\r\nX-Otro: 1", "cl\u0000ave")) {
            val r = try { ClienteAsistente(url(), mala, Duration.ofSeconds(1), pausaReintento = 1).preguntar("hola", emptyList()) } catch (e: Throwable) { fail("clave con caracteres de control lanzó ${e::class.simpleName}") }
            assertTrue(r is Resultado.Falla || r is Resultado.Respuesta)
        }
    }

    private fun fail(m: String): Nothing = throw AssertionError(m)

    @Test
    fun `el contexto nunca pasa de 1000 caracteres por linea con el prefijo`() = runBlocking {
        cuerpo = """{"respuesta":"${"r".repeat(3000)}","conversacionId":"c-1"}"""
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val c = ControladorAsistente(cliente(), SinVoz, alcance, vozActiva = { false })
            repeat(5) {
                assertTrue(c.enviar("p$it " + "z".repeat(1500)))
                val libre = withTimeoutOrNull(10_000) { while (c.esperando) delay(20); true }
                assertEquals(true, libre, "el chat debe liberarse")
            }
            val ultimo = Json.parseToJsonElement(cuerpos.last()).jsonObject["contexto"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertTrue(ultimo.isNotEmpty() && ultimo.size <= 6)
            assertTrue(ultimo.all { it.length <= 1000 }, "el servidor rechaza líneas de más de 1000; tamaños: ${ultimo.map { it.length }}")
        } finally {
            alcance.cancel()
        }
    }

    @Test
    fun `preferencias manipuladas a mano no rompen la app`() {
        val archivo = Files.createTempFile("pref", ".properties")
        try {
            Files.writeString(
                archivo,
                "escalaTexto=NaN\nvelocidadVoz=99999999999999999999\nvozActiva=quizas\naceptacion.version=\naceptacion.fecha=ayer\naceptacion.mayorDeEdad=si\n",
            )
            val p = Preferencias(archivo)
            assertTrue(p.escalaTexto.isFinite() && p.escalaTexto in 1f..2f, "escala debe ser finita y de 1 a 2, fue ${p.escalaTexto}")
            assertEquals(0, p.velocidadVoz)
            assertTrue(p.vozActiva)
            assertNull(p.aceptacion, "una constancia con fecha inválida no vale")

            Files.writeString(archivo, "escalaTexto=Infinity\n")
            assertTrue(Preferencias(archivo).escalaTexto.isFinite())
            Files.write(archivo, ByteArray(5000) { (it * 31).toByte() })
            assertNull(Preferencias(archivo).aceptacion, "un archivo binario se ignora")
            Files.writeString(archivo, "aceptacion.version=1\naceptacion.fecha=2999-01-01T00:00:00Z\naceptacion.mayorDeEdad=true\n")
            // Quien edite la fecha a mano obtiene una constancia que existe; la validez la decide la versión, no la fecha.
            assertEquals("1", Preferencias(archivo).aceptacion?.version)
        } finally {
            Files.deleteIfExists(archivo)
        }
    }
}
