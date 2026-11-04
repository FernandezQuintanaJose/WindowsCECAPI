# Entrega de la app de Windows (4 nov 2026)

**Qué hace cada quien esta semana: [TAREAS.md](TAREAS.md).**

Revisión completa, hallazgos de todo el ecosistema y orden sugerido para la semana: [docs/estado/2026-11-04-revision-qa-y-entrega.md](https://github.com/FernandezQuintanaJose/EstadiasAppMobil/blob/docs/revision-qa-4nov/docs/estado/2026-11-04-revision-qa-y-entrega.md) en EstadiasAppMobil (rama `docs/revision-qa-4nov`).

## Estado
- 23 pruebas correctas con JDK 21 (`./gradlew test`), 9 de ellas nuevas en `HostilTest`.
- Corregido: una dirección o clave del servidor mal escrita ya no deja la conversación esperando para siempre; una respuesta en blanco se avisa; las líneas de contexto respetan el límite de 1000 del servidor; `escalaTexto=NaN` se ignora.
- **No se ha comprobado:** que el flujo `windows.yml` genere el `.exe`, que se instale en un equipo limpio, la voz en español ni los lectores de pantalla.

## Para entregar
1. ~~Fusionar `qa/correcciones` a `main`~~ (hecho el 4 nov 2026).
2. GitHub, Settings, Secrets and variables, Actions: crear `CECAPI_SERVIDOR` y `CECAPI_CLAVE_APP` (cuando el servidor esté publicado).
3. Ver correr `windows.yml` en GitHub e instalar el artefacto en un equipo Windows limpio.
4. Probar: voz de Windows en español (Ajustes, Probar la voz), NVDA y JAWS (Java Access Bridge, `jabswitch -enable`) y Narrador.
5. Decidir el certificado de firma. Sin él, SmartScreen mostrará un aviso.
6. Subir la versión en `build.gradle.kts`, crear la etiqueta (`git tag v1.0.1 && git push origin v1.0.1`) y comprobar el Release con su SHA-256.
7. Registrar versión, archivo, url, tamaño y SHA-256 en `datos/descargas.v1.json` de PaginaAsistenteCECAPI. El enlace debe ser de `github.com/FernandezQuintanaJose/WindowsCECAPI/releases/`, o la página lo descarta.
8. Pendientes de funciones: Consultas (`/documento`), abrir foto para Entorno (`/ver`) y decidir si las cuentas son locales o del servidor (`docs/FUNCIONES.md`).
