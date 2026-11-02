import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Versiones congeladas: las mismas de Kotlin que la app Android (decisión 0001 de EstadiasAppMobil).
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
}

group = "mx.cecapi"
// Windows exige versión numérica MAYOR.MENOR.PARCHE con MAYOR > 0 para el instalador.
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

// ---- Configuración del servidor ----------------------------------------------------------------
// Igual que en la app Android: la dirección del servidor y la clave salen de local.properties
// (que no se sube al repositorio) o, en GitHub Actions, de variables de entorno con los secretos.
//   cecapi.servidor=https://<nombre>.onrender.com
//   cecapi.claveApp=<la CECAPI_CLAVE_APP de Render>
// La clave dentro del .exe se puede sacar: frena el uso casual, no a alguien decidido.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun valorDeConfiguracion(propiedad: String, variable: String): String =
    (propiedadesLocales.getProperty(propiedad) ?: System.getenv(variable) ?: "").trim()

fun comoTextoKotlin(valor: String): String =
    "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""

val generarConfiguracion by tasks.registering {
    val servidor = valorDeConfiguracion("cecapi.servidor", "CECAPI_SERVIDOR")
    val clave = valorDeConfiguracion("cecapi.claveApp", "CECAPI_CLAVE_APP")
    val versionApp = project.version.toString()
    val salida = layout.buildDirectory.dir("generated/configuracion")
    inputs.property("servidor", servidor)
    inputs.property("clave", clave)
    inputs.property("version", versionApp)
    outputs.dir(salida)
    doLast {
        val archivo = salida.get().file("mx/cecapi/escritorio/ConfiguracionGenerada.kt").asFile
        archivo.parentFile.mkdirs()
        archivo.writeText(
            """
            |// Archivo generado por la tarea generarConfiguracion. No editar.
            |package mx.cecapi.escritorio
            |
            |internal object ConfiguracionGenerada {
            |    const val SERVIDOR = ${comoTextoKotlin(servidor)}
            |    const val CLAVE_APP = ${comoTextoKotlin(clave)}
            |    const val VERSION = ${comoTextoKotlin(versionApp)}
            |}
            |""".trimMargin(),
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generarConfiguracion)
}

tasks.test {
    useJUnit()
    // `gradle test -Pcapturas` guarda imágenes de cada pantalla en build/capturas para revisarlas.
    systemProperty("capturas", if (project.hasProperty("capturas")) "si" else "no")
    systemProperty("carpetaCapturas", layout.buildDirectory.dir("capturas").get().asFile.absolutePath)
}

// ---- Empaquetado ---------------------------------------------------------------------------------
// Se distribuye solo como .exe por GitHub Releases (decisión del 30 oct 2026, sin Microsoft Store).
// El .exe solo se puede generar en Windows (jpackage no compila para otro sistema): lo hace el
// flujo .github/workflows/windows.yml en un equipo Windows de GitHub.
compose.desktop {
    application {
        mainClass = "mx.cecapi.escritorio.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "CECAPI"
            packageVersion = project.version.toString()
            description = "Asistente de CECAPI para personas ciegas y con baja visión"
            vendor = "Centro de Capacitación para Invidentes A. C."
            // java.net.http: cliente del servidor. jdk.accessibility: puente para lectores de pantalla.
            modules("java.net.http", "jdk.accessibility")
            windows {
                menuGroup = "CECAPI"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                // Fijo para siempre: así Windows reconoce las versiones nuevas como actualización.
                upgradeUuid = "4cc49578-9eb0-47f8-8b35-f9603831d31c"
            }
        }
    }
}
