package mx.cecapi.escritorio

import kotlin.test.*
import kotlinx.coroutines.runBlocking

class MicrofonoYServidorTest {
    @Test fun comandosExactos() {
        assertEquals(ComandoVoz.CALLAR, identificarComando("¡Silencio!"))
        assertEquals(ComandoVoz.RAPIDA, identificarComando("Habla más rápido."))
        assertEquals(ComandoVoz.SUBIR_VOLUMEN, identificarComando("SUBE EL VOLUMEN"))
        assertEquals(ComandoVoz.ENVIAR, identificarComando("Envía la pregunta"))
        assertNull(identificarComando("¿Cómo se sube el volumen de una computadora?"))
        assertNull(identificarComando("¿Quién fue Louis Braille?"))
    }
    @Test fun contratoHttpLocal() = runBlocking {
        ServidorPrueba().use { servidor ->
            val cliente = ClienteAsistente(servidor.url, "")
            assertTrue(cliente.despertar())
            val resultado = assertIs<Resultado.Respuesta>(cliente.preguntar("Hola", emptyList()))
            assertEquals("simulado-local", resultado.proveedor)
            assertTrue(resultado.texto.contains("simulada"))
            val id = assertNotNull(cliente.conversacionId)
            assertIs<Resultado.Respuesta>(cliente.preguntar("Otra pregunta", listOf("Persona: Hola")))
            assertEquals(id, cliente.conversacionId)
            assertIs<Resultado.Falla>(cliente.preguntar("", emptyList()))
        }
        Unit
    }
}
