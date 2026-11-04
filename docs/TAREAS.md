# Tareas del equipo — CECAPI para Windows

Semana del 4 al 11 nov 2026. Cada tarea dice qué hacer, dónde y cuándo está terminada. Lo que no se pudo comprobar se marca **SIN VERIFICAR**: no se inventa.

## Cómo empezar (20 min)

Requiere JDK 21 y Windows 10 u 11 para el instalador.

```
git clone https://github.com/FernandezQuintanaJose/WindowsCECAPI.git
cd WindowsCECAPI
git checkout qa/correcciones                # hasta que se fusione a main
./gradlew test                              # 23 pruebas, deben pasar
./gradlew run                               # abre la app
```

Para hablar con el servidor real, crear `local.properties` (no se sube al repositorio, nunca copiarlo a un chat ni a un commit):

```
cecapi.servidor=https://<nombre>.onrender.com
cecapi.claveApp=<la CECAPI_CLAVE_APP de Render>
```

Reglas de trabajo:
- Una rama por tarea: `tarea/<nombre-corto>`. Pull request a `main`. No se sube directo a `main`.
- Antes de cada pull request: `./gradlew test` sin fallas.
- Cada función nueva lleva su prueba (ver `ClienteAsistenteTest`, `HostilTest`).
- La interfaz es para personas ciegas y con baja visión: todo se alcanza con teclado (Tab, Enter, Esc), el foco se ve grueso y de color, y todo lo que se muestra también se dice por voz o lo lee el lector de pantalla.
- Qué entra y qué se oculta en una computadora está en [FUNCIONES.md](FUNCIONES.md). No agregar nada que no esté ahí sin actualizarlo.

## Prioridad 1 — Sin esto no se puede entregar

| # | Tarea | Dónde | Hecho cuando |
|---|---|---|---|
| W1 | **Comprobar el instalador**: correr `windows.yml` en GitHub (o `./gradlew packageExe` en Windows) e instalar el `.exe` en un equipo limpio, sin JDK. | `.github/workflows/windows.yml`, `build.gradle.kts` | Se instala sin permisos de administrador, abre, aparece en el menú Inicio y se desinstala. Anotar versión de Windows probada. **SIN VERIFICAR hoy.** |
| W2 | **Voz de Windows en español**: confirmar que existe una voz en español (de México, si está) y que la app la elige. Si el equipo no la tiene, la app debe avisar cómo instalarla. | `src/main/kotlin/mx/cecapi/escritorio/Voz.kt` | Probada en 2 equipos. **SIN VERIFICAR hoy.** |
| W3 | **Lectores de pantalla**: NVDA y JAWS (activar Java Access Bridge con `jabswitch -enable`) y Narrador. Recorrer políticas, asistente y ajustes. | Todas las pantallas | Documento `docs/LECTORES.md` con qué se escucha, qué no y qué falta. Corregir lo que no tenga nombre accesible. Narrador probablemente no lea Compose: si no la lee, decirlo claramente en el README. |
| W4 | **Cuentas**: decidir con la gente del servidor si Windows usa cuentas locales o las del servidor. El texto de políticas ya dice que el asistente se usa con una cuenta. | `Politicas.kt`, `docs/FUNCIONES.md` | Decisión escrita. Mientras no haya cuentas, ajustar el texto de las políticas para que no mienta. |
| W5 | **Secretos y versión**: crear `CECAPI_SERVIDOR` y `CECAPI_CLAVE_APP` en Settings, Secrets, Actions cuando el servidor esté publicado; subir versión en `build.gradle.kts`; etiqueta `v1.0.1`; comprobar el Release y su SHA-256. | GitHub | El Release tiene el `.exe`, y su SHA-256 coincide al descargarlo (`Get-FileHash`). |
| W6 | **Registrar la descarga** en `datos/descargas.v1.json` de PaginaAsistenteCECAPI (versión, archivo, url, tamaño, SHA-256). La url debe ser de `github.com/FernandezQuintanaJose/WindowsCECAPI/releases/`. | Repositorio del asistente web | La página de descargas muestra el botón con el tamaño y el SHA-256. |

## Prioridad 2 — Funciones pendientes de [FUNCIONES.md](FUNCIONES.md)

Orden sugerido (de menos a más trabajo). Cada una necesita su prueba y su descripción hablada.

| # | Tarea | Notas | Hecho cuando |
|---|---|---|---|
| F1 | **Consultas** (`POST /documento`): redactar documentos. Es texto, se lleva igual que en Android. | Reusar `ClienteAsistente.kt`; mismos límites y mensajes de error que `/preguntar`. | Pantalla alcanzable por teclado, con voz y con pruebas contra el servidor simulado. |
| F2 | **Entorno por foto** (`POST /ver`): abrir una imagen desde el explorador de archivos y mandarla. | Límite de tamaño y tipo (solo imagen) antes de enviar. **No** acceder a la cámara. | Una imagen demasiado grande o de otro tipo se rechaza con mensaje claro y voz. |
| F3 | **Chats / historial, Clasificación, Gestión, Soporte**: dependen de W4 (cuentas). | No empezar sin la decisión de W4. | — |
| F4 | **Actividades de sonido**: faltan audios y niveles. | Requiere audífonos. Revisar con CECAPI si se quiere. | Pedir a CECAPI antes de empezar. |

Las funciones ocultas (cámara, vibración, llamadas, notificaciones) **no se hacen** en Windows.

## Prioridad 3 — Calidad

- **Reconocimiento de voz** para mandar la pregunta hablando: **SIN VERIFICAR** que funcione en español y sin conexión en Windows. Probar antes de prometerlo; si no sirve, dejarlo oculto.
- **Certificado de firma**: sin él, SmartScreen avisa que el instalador es desconocido. Decidir con José si se compra; mientras tanto, documentar el aviso para la gente que lo instale.
- Probar el cambio de tamaño de ventana y el 200 % de texto en pantallas pequeñas (1366×768).
- Cuando la app esté en producción: vigilar que `preferencias.properties` no guarde preguntas ni respuestas (hoy no las guarda; hay prueba).

## Conectar con los demás equipos

- **Servidor** (EstadiasAppMobil, `backend/`): `/preguntar`, `/documento`, `/ver`. Hallazgos abiertos del servidor en `docs/estado/2026-11-04-revision-qa-y-entrega.md` de ese repositorio.
- **Asistente web**: hospeda la página de descargas del `.exe` (W6).

## Entregable final

`.exe` en un Release con SHA-256, `./gradlew test` sin fallas, `docs/ENTREGA.md` al día, `docs/LECTORES.md` con lo que se probó, lista de lo pendiente y por qué.
