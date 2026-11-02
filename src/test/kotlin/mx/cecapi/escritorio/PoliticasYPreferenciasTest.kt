package mx.cecapi.escritorio

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LectorDePoliticasTest {
    @Test
    fun `no deja aceptar sin llegar al final ni sin declarar 18 anos`() {
        val lector = LectorDePoliticas(total = 3)
        assertNotNull(lector.faltaParaAceptar()).also { assertTrue("última parte" in it) }
        lector.mayorDeEdad = true
        assertTrue("última parte" in lector.faltaParaAceptar()!!, "marcar 18+ no salta la lectura")

        assertTrue(lector.siguiente())
        assertTrue(lector.siguiente())
        assertTrue(lector.esUltima)
        assertFalse(lector.siguiente(), "no pasa de la última")
        assertNull(lector.faltaParaAceptar())

        lector.mayorDeEdad = false
        assertTrue("18 años" in lector.faltaParaAceptar()!!)
    }

    @Test
    fun `regresar no borra lo ya leido`() {
        val lector = LectorDePoliticas(total = 2)
        lector.siguiente()
        assertTrue(lector.anterior())
        assertFalse(lector.anterior())
        lector.mayorDeEdad = true
        assertNull(lector.faltaParaAceptar(), "ya había llegado al final")
    }

    @Test
    fun `las politicas tienen partes y version`() {
        assertTrue(Politicas.partes.size >= 10)
        assertTrue(Politicas.partes.all { it.titulo.isNotBlank() && it.parrafos.isNotEmpty() })
        assertTrue(Politicas.VERSION.isNotBlank())
    }
}

class PreferenciasTest {
    @Test
    fun `guarda y lee ajustes y aceptacion`() {
        val carpeta = Files.createTempDirectory("cecapi")
        val archivo = carpeta.resolve("sub/preferencias.properties")
        val p = Preferencias(archivo)
        assertEquals(1f, p.escalaTexto)
        assertTrue(p.vozActiva)
        assertFalse(p.aceptoVersion(Politicas.VERSION))

        p.escalaTexto = 1.5f
        p.vozActiva = false
        p.velocidadVoz = 9
        val fecha = Instant.parse("2026-11-02T18:00:00Z")
        p.aceptacion = Aceptacion(Politicas.VERSION, fecha, mayorDeEdad = true)

        val otra = Preferencias(archivo)
        assertEquals(1.5f, otra.escalaTexto)
        assertFalse(otra.vozActiva)
        assertEquals(5, otra.velocidadVoz, "la velocidad se limita a 5")
        assertEquals(Aceptacion(Politicas.VERSION, fecha, true), otra.aceptacion)
        assertTrue(otra.aceptoVersion(Politicas.VERSION))
        assertFalse(otra.aceptoVersion("otra-version"), "una versión nueva pide aceptar otra vez")

        otra.aceptacion = null
        assertNull(Preferencias(archivo).aceptacion)
    }

    @Test
    fun `un archivo danado no impide abrir la app`() {
        val archivo = Files.createTempFile("cecapi", ".properties")
        Files.write(archivo, byteArrayOf(0x5c, 0x75, 0x5a)) // "\uZ": escape inválido
        val p = Preferencias(archivo)
        assertEquals(1f, p.escalaTexto)
        assertNull(p.aceptacion)
    }
}
