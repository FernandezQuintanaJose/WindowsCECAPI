package mx.cecapi.escritorio

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Los textos que se dicen en voz alta cuando algo falla. Son los mismos de la app Android
 * (`AiMessages` en EstadiasAppMobil), para que las dos apps digan lo mismo.
 */
object Mensajes {
    const val DEMASIADAS = "Hiciste muchas preguntas seguidas. Espera un minuto y vuelve a intentar."
    const val NO_DISPONIBLE = "El asistente no está disponible en este momento. Intenta más tarde."
    const val SIN_CONEXION = "No pude conectar con el asistente inteligente. Intenta de nuevo en un momento."
    const val RECHAZADA = "El asistente no entendió esa solicitud. Prueba decirlo de otra forma."
    const val NO_AUTORIZADA = "El servidor del asistente no reconoce a esta versión de la app. Avisa a quien te la instaló."
    const val DESPERTANDO = "El servidor estaba dormido y está despertando. Puede tardar hasta un minuto. Sigo aquí."
    const val SIN_SERVIDOR = "Esta versión de la app no tiene configurado el servidor del asistente."
    const val PREGUNTA_INVALIDA = "La pregunta no es válida. Revisa que no esté vacía y que tenga menos de 2,000 caracteres."
}

/** Lo que pasó al preguntar. */
sealed interface Resultado {
    data class Respuesta(val texto: String, val hayMas: Boolean, val proveedor: String?) : Resultado
    data class Falla(val mensaje: String) : Resultado
}

@Serializable
private data class PeticionPregunta(
    val pregunta: String,
    val contexto: List<String>,
    val conversacionId: String? = null,
)

@Serializable
private data class RespuestaServidor(
    val respuesta: String,
    val proveedor: String? = null,
    val hayMas: Boolean = false,
    val conversacionId: String? = null,
)

/**
 * Cliente del servidor del asistente (FastAPI en Render). Mismo contrato que la app Android:
 * `POST /preguntar` con `{ pregunta, contexto, conversacionId }` y el encabezado `X-Clave-App`
 * si hay clave. `GET /` despierta al servidor sin tocar la base de datos.
 *
 * Los tiempos son los de Android: 15 s para conectar y 75 s para responder, porque Render
 * gratuito tarda cerca de un minuto en despertar. Si no hubo respuesta se intenta una vez más;
 * si el servidor sí contestó con error (429, 503...), no.
 */
class ClienteAsistente(
    servidor: String,
    private val clave: String,
    private val esperaRespuesta: Duration = Duration.ofSeconds(75),
    private val pausaReintento: Long = 2_000,
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
) {
    // Se acepta el servidor ("https://x.onrender.com") o la URL completa anterior (".../preguntar").
    private val base = servidor.trim().trimEnd('/').removeSuffix("/preguntar")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** La conversación en curso: el servidor la devuelve y se manda en la siguiente pregunta. */
    var conversacionId: String? = null
        private set

    val configurado: Boolean get() = base.startsWith("http")

    fun nuevaConversacion() {
        conversacionId = null
    }

    /** Despierta al servidor. true si contestó. */
    suspend fun despertar(): Boolean {
        if (!configurado) return false
        return try {
            val peticion = HttpRequest.newBuilder(URI.create("$base/")).timeout(esperaRespuesta).GET().build()
            http.sendAsync(peticion, HttpResponse.BodyHandlers.discarding()).await().statusCode() in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun preguntar(pregunta: String, contexto: List<String>): Resultado = withContext(Dispatchers.IO) {
        if (!configurado) return@withContext Resultado.Falla(Mensajes.SIN_SERVIDOR)
        val cuerpo = json.encodeToString(
            PeticionPregunta.serializer(),
            PeticionPregunta(pregunta = pregunta, contexto = contexto.takeLast(20), conversacionId = conversacionId),
        )
        val peticion = HttpRequest.newBuilder(URI.create("$base/preguntar"))
            .timeout(esperaRespuesta)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .apply { if (clave.isNotBlank()) header("X-Clave-App", clave) }
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build()

        repeat(INTENTOS) { intento ->
            try {
                val respuesta = http.sendAsync(peticion, HttpResponse.BodyHandlers.ofString()).await()
                return@withContext interpretar(respuesta.statusCode(), respuesta.body())
            } catch (e: Exception) {
                if (e !is IOException && e !is HttpTimeoutException && e.cause !is IOException) throw e
                if (intento < INTENTOS - 1) delay(pausaReintento)
            }
        }
        Resultado.Falla(Mensajes.SIN_CONEXION)
    }

    private fun interpretar(codigo: Int, cuerpo: String): Resultado {
        if (codigo in 200..299) {
            val datos = try {
                json.decodeFromString(RespuestaServidor.serializer(), cuerpo)
            } catch (e: Exception) {
                return Resultado.Falla(Mensajes.NO_DISPONIBLE)
            }
            conversacionId = datos.conversacionId ?: conversacionId
            return Resultado.Respuesta(datos.respuesta, datos.hayMas, datos.proveedor)
        }
        val detalle = detalleDeError(cuerpo)
        return Resultado.Falla(
            when (codigo) {
                400 -> detalle ?: Mensajes.RECHAZADA
                401 -> Mensajes.NO_AUTORIZADA
                422 -> Mensajes.PREGUNTA_INVALIDA
                429 -> Mensajes.DEMASIADAS
                503 -> Mensajes.NO_DISPONIBLE
                else -> Mensajes.SIN_CONEXION
            },
        )
    }

    /** El `detail` de un error de FastAPI cuando es una frase (en un 422 es una lista y no se usa). */
    private fun detalleDeError(cuerpo: String): String? = try {
        (json.parseToJsonElement(cuerpo).jsonObject["detail"] as? JsonPrimitive)
            ?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val INTENTOS = 2
    }
}
