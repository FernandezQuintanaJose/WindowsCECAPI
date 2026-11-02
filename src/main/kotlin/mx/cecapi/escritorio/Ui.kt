package mx.cecapi.escritorio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Misma paleta que la app Android (core/theme/Color.kt de EstadiasAppMobil): oscura, de alto contraste
// y fija, sin seguir el tema del sistema, porque para baja visión importa más la constancia.
object Colores {
    val Fondo = Color(0xFF121212)
    val Superficie = Color(0xFF1E1E1E)
    val SuperficieElevada = Color(0xFF2A2A2A)
    val Borde = Color(0xFF3A3A3A)
    val Acento = Color(0xFF38BDF8)
    val Texto = Color(0xFFFFFFFF)
    val TextoTenue = Color(0xFFC7C7C7)
    val Exito = Color(0xFF30D158)
    val Error = Color(0xFFFF453A)
    val Aviso = Color(0xFFF59E0B)
}

private val tipografia = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
)

/** Tema de la app. [escalaTexto] agranda todo el texto (1 = normal, 2 = el doble). */
@Composable
fun TemaCecapi(escalaTexto: Float, contenido: @Composable () -> Unit) {
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, densidad.fontScale * escalaTexto)) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Colores.Acento,
                onPrimary = Colores.Fondo,
                background = Colores.Fondo,
                onBackground = Colores.Texto,
                surface = Colores.Superficie,
                onSurface = Colores.Texto,
                surfaceVariant = Colores.SuperficieElevada,
                onSurfaceVariant = Colores.TextoTenue,
                outline = Colores.Borde,
                error = Colores.Error,
            ),
            typography = tipografia,
        ) {
            // El fondo y el color del texto se fijan aquí, para que ninguna pantalla quede en blanco y negro.
            Surface(Modifier.fillMaxSize(), color = Colores.Fondo, contentColor = Colores.Texto) { contenido() }
        }
    }
}

/**
 * Botón grande de la app. Cuando tiene el foco (Tab, flechas o un lector de pantalla que mueve el
 * foco) se ilumina: borde grueso y fondo con su [color]. Así quien tiene baja visión ve dónde está,
 * como el recuadro de TalkBack en el teléfono.
 */
@Composable
fun BotonGrande(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    principal: Boolean = false,
    color: Color = Colores.Acento,
    habilitado: Boolean = true,
) {
    val fuente = remember { MutableInteractionSource() }
    val enfocado by fuente.collectIsFocusedAsState()
    val forma = RoundedCornerShape(16.dp)
    val fondo = when {
        principal -> color
        enfocado -> color.copy(alpha = 0.22f)
        else -> Colores.Superficie
    }
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = forma,
        interactionSource = fuente,
        border = when {
            enfocado -> BorderStroke(4.dp, if (principal) Colores.Texto else color)
            principal -> null
            else -> BorderStroke(1.dp, Colores.Borde)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo,
            contentColor = if (principal) Colores.Fondo else Colores.Texto,
            disabledContainerColor = Colores.Superficie,
            disabledContentColor = Colores.TextoTenue,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Fracción de la ventana para el panel de acciones: la misma que el micrófono en el teléfono. */
const val FRACCION_PANEL = 0.42f

/**
 * Ventana dividida como la app Android: el contenido (que se desplaza) ocupa el 58 % y el panel de
 * acciones el 42 %. En ventanas anchas el panel va a la derecha; en angostas, abajo.
 */
@Composable
fun PantallaDividida(contenido: @Composable () -> Unit, panel: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f - FRACCION_PANEL).fillMaxHeight()) { contenido() }
                Column(Modifier.weight(FRACCION_PANEL).fillMaxHeight()) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f - FRACCION_PANEL).fillMaxWidth()) { contenido() }
                Column(Modifier.weight(FRACCION_PANEL).fillMaxWidth()) { panel() }
            }
        }
    }
}
