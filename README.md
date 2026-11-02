# CECAPI para Windows

App de escritorio del asistente de CECAPI para personas ciegas y con baja visión. Es la app de Android llevada a Windows; lo que no se puede hacer en una computadora se oculta (ver `docs/FUNCIONES.md`). Se distribuye solo como **instalador `.exe` en GitHub Releases**, sin Microsoft Store.

La ficha del proyecto está en `docs/FICHA.md` y las reglas comunes en `docs/FICHA_GLOBAL.md`.

## Qué hace (versión 1.0.0)

- **Políticas por partes**, con voz y declaración de 18 años o más, antes de usar el asistente. El mismo texto que la página web.
- **Asistente:** escribes una pregunta, se manda al servidor (`POST /preguntar`, el mismo de la app Android) y la respuesta se muestra y se lee en voz alta. Sigue la misma conversación (`conversacionId`) y manda las últimas 6 líneas como contexto.
- **Servidor dormido:** al abrir la app se despierta el servidor (`GET /`). Si una respuesta tarda más de 4 segundos, avisa por voz que está despertando. Espera hasta 75 segundos, como en Android.
- **Pantalla dividida** como en el teléfono: el contenido en el 58 % y el panel de acciones en el 42 % (a la derecha en ventanas anchas, abajo en angostas).
- **Teclado:** Tab se mueve entre botones y el que tiene el foco se ilumina. Enter envía la pregunta (Mayúsculas + Enter hace salto de línea). **Ctrl + R** repite la última respuesta y **Esc** calla la voz.
- **Ajustes:** tamaño del texto (100 a 200 %), voz encendida o apagada y velocidad.
- Las preferencias y la constancia de aceptación se guardan en `%APPDATA%\CECAPI\preferencias.properties`. No se guardan preguntas ni respuestas en la computadora.

## Configurar el servidor

Igual que la app Android. En `local.properties`, que no se sube al repositorio:

```
cecapi.servidor=https://<nombre>.onrender.com
cecapi.claveApp=<la CECAPI_CLAVE_APP de Render>
```

Sin estos datos la app abre igual y avisa que no tiene servidor. La clave queda dentro del `.exe`: frena el uso casual, no a alguien decidido (ver `backend/PUBLICAR.md` en EstadiasAppMobil).

## Compilar y probar

Requiere JDK 21. El primer comando descarga Gradle y las dependencias.

```
./gradlew run                 # abre la app (en Windows, macOS o Linux)
./gradlew test                # pruebas: cliente del servidor, políticas y preferencias
./gradlew test -Pcapturas     # además guarda una imagen de cada pantalla en build/capturas
./gradlew packageExe          # instalador .exe (solo en Windows)
```

El instalador queda en `build/compose/binaries/main/exe/CECAPI-1.0.0.exe`. Se instala por usuario, sin permisos de administrador, y crea acceso en el menú Inicio.

## Publicar una versión

1. Una sola vez: en GitHub, **Settings > Secrets and variables > Actions**, crear los secretos `CECAPI_SERVIDOR` y `CECAPI_CLAVE_APP`.
2. Subir la versión en `build.gradle.kts` (`version = "1.0.1"`; Windows exige que el primer número sea mayor que 0).
3. Crear y subir la etiqueta: `git tag v1.0.1 && git push origin v1.0.1`.
4. El flujo `.github/workflows/windows.yml` corre las pruebas, genera el `.exe` en un equipo Windows y lo publica en Releases con su suma SHA-256.
5. Agregar la versión en `datos/descargas.v1.json` de PaginaAsistenteCECAPI.

En cada cambio a `main` el flujo también genera el `.exe` y lo deja como artefacto descargable de la ejecución, para probarlo antes de publicar.

## Lo que falta verificar (no se pudo hacer sin Windows)

| Qué | Cómo |
|---|---|
| Que el `.exe` se genere e instale en un equipo limpio (ficha, tarea 1) | Ver la primera ejecución del flujo en GitHub e instalar el artefacto |
| Voz de Windows en español | Ajustes > Probar la voz. Si no hay voz en español, Windows usa la que tenga; se agregan en Configuración de Windows > Hora e idioma > Voz |
| Lectores de pantalla | Probar con NVDA y JAWS. Compose para escritorio usa Java Access Bridge, que se activa con `jabswitch -enable`. Es probable que Narrador no lea la app |
| Aviso de SmartScreen | Esperado mientras la app sea nueva y no tenga certificado de firma |

## Lo que sigue

Ver `docs/FUNCIONES.md`. Lo más directo: **Consultas** (`/documento`) y abrir una foto para **Entorno** (`/ver`). Las cuentas dependen de una decisión: cuentas locales en Windows o cuentas en el servidor.
