# Funciones de la app móvil y su estado en Windows

Tarea 2 de la ficha. Fecha: 2 nov 2026. Comparado contra `EstadiasAppMobil` en `main` (`b8e39d8`).

Estados: **Igual** (hace lo mismo), **Adaptada** (lo mismo con otra forma de usarse en una computadora), **Pendiente** (se puede llevar a Windows, falta hacerlo), **Oculta** (no se puede o no tiene sentido en una computadora por ahora).

Criterio mínimo de aceptación de Windows (lista de verificación, punto 21): **acceso a la inteligencia artificial**. Los demás paneles son opcionales.

| Función en el teléfono | Pantalla en Android | Windows | Por qué |
|---|---|---|---|
| Políticas por partes, con voz y declaración de 18 años o más | (aceptación antes de la IA) | **Igual** | Mismo texto que la web (`Politicas.kt`), mismas reglas para aceptar |
| Asistente Inteligente: preguntas libres a la IA | `AiAssistantScreen` | **Igual** (por texto) | `POST /preguntar` con `X-Clave-App`, mismos mensajes de error y mismos tiempos (75 s) |
| Aviso de servidor despertando y voz de espera | `AiMessages.WAKING` | **Igual** | Se dice a los 4 s sin respuesta |
| Leer las respuestas en voz alta | voz de Android | **Adaptada** | Voz de Windows (System.Speech con PowerShell), prefiere español de México |
| Micrófono como control principal | `MicPad` | **Adaptada** | Panel de acciones en el 42 % de la ventana (Repetir, Callar, Nueva conversación, Ajustes) |
| Tarjeta iluminada donde está la persona | `BigBtn` seleccionada | **Adaptada** | El botón con el foco (Tab) se ilumina con borde grueso y color |
| Repetir la última respuesta / callar | doble toque, dos dedos | **Adaptada** | Ctrl + R y Esc, y botones |
| Tamaño de texto, velocidad de la voz | Personalización, Configuración | **Adaptada** | Pantalla Ajustes |
| Asistente de Voz: comandos por voz y palabra de activación | `VoiceAssistantScreen` | **Oculta** | Reconocimiento de voz en español en Windows sin conexión: SIN VERIFICAR (ficha, tarea 6) |
| Consultas: redactar documentos | `ConsultasScreen` (`/documento`) | **Pendiente** | Es texto: se puede llevar tal cual. Candidata para la siguiente versión |
| Asistente del Entorno y billetes | `EnvironmentScreen` (`/ver`) | **Pendiente** (adaptada) | Sin cámara garantizada; se podría abrir una foto desde un archivo y mandarla a `/ver` |
| Cámara Inteligente / lector de documentos | `DocumentReaderScreen` | **Oculta** | Usa la cámara y OCR del teléfono |
| Actividades: sonidos (audio espacial) | `LearningScreen` | **Pendiente** | Funciona con audífonos; faltan los audios y la lógica de niveles |
| Actividades: vibración | `VibrationScreen` | **Oculta** | Una computadora no vibra |
| Actividades: juegos con cámara | `JuegoCamaraScreen`, `JuegoScreen` | **Oculta** | Usan la cámara del teléfono |
| Iniciar sesión, crear cuenta, mi cuenta | `LoginScreen`, `RegisterScreen`, `EditAccountScreen` | **Pendiente** | En Android las cuentas viven en el teléfono (`ServicioDeCuentas` / `CuentasLocales`), no en el servidor. Hay que decidir si Windows tiene cuentas locales propias o si se esperan las cuentas del servidor |
| Chats (historial) | `ChatsScreen` | **Pendiente** | Depende de las cuentas y del historial del servidor |
| Clasificación, Gestión, Soporte, Reportar | `RankingScreen`, `GestionScreen`, `SoporteScreen` | **Pendiente** | Dependen de las cuentas |
| Emergencias (llamar a contactos) | `EmergenciasScreen` | **Oculta** | Una computadora no llama por teléfono |
| Escuchar fuera de la app, avisos de notificaciones | servicios de Android | **Oculta** | Son funciones del sistema Android |
| Lector de pantalla del sistema | TalkBack | **Adaptada, sin verificar** | Compose para escritorio se comunica con NVDA y JAWS por Java Access Bridge. Narrador de Windows probablemente no la lea. Hay que probarlo |

## Diferencias que hay que cuidar

- **Políticas:** el texto dice que el asistente se usa con una cuenta, pero Windows todavía no tiene cuentas (igual que la web mientras el servidor no tenga cuentas). Hay que ajustar el texto o agregar las cuentas antes de publicar.
- **Clave de la app:** va dentro del `.exe`, como en el APK. Frena el uso casual, no a alguien decidido (`backend/PUBLICAR.md`).
