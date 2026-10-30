# Ficha: App Windows (WindowsCECAPI)

Responsable: Alonso. Complementa la ficha global (`00_global`). El repositorio está vacío.

## Objetivo
La misma app móvil llevada a Windows, ocultando o desactivando lo que no se pueda hacer.

## Tecnología candidata
Kotlin con Compose Desktop (genera MSI, EXE, DEB). Empaquetado como `.exe` (Compose Desktop usa jpackage): SIN VERIFICAR para esta app, es el primer experimento. [REEMPLAZADO el 30 oct: antes era MSIX para la Microsoft Store.] Reutilizar la lógica común con la app Android donde sea posible.

## Distribución
- Único canal: archivo `.exe` por descarga directa desde GitHub Releases (decisión del 30 oct 2026). **Microsoft Store descartada definitivamente.** [REEMPLAZADO: antes la Store era el canal principal.]
- Windows puede mostrar alertas de SmartScreen por falta de reputación. Certificado de firma: SIN DECIDIR. Linux (DEB): ya no es respaldo; queda fuera salvo que se pida.
- Evitar empaquetar con PyInstaller por falsos positivos de antivirus.

## Tareas, en orden
1. Prueba técnica: app mínima en Compose Desktop empaquetada como `.exe` e instalada en un equipo limpio. Resultado: sí/no.
2. `FUNCIONES.md`: tabla de funciones de la app móvil (por módulo) con estado en Windows: igual, adaptada, oculta.
3. Detección de capacidades para ocultar lo no soportado.
4. Cliente de la API con la misma cuenta propia (usuario o correo y contraseña), token, borrar chats y eliminar cuenta, como la app móvil.
5. Pantallas de políticas con audio.
6. Voz: reconocimiento y síntesis en español; sin conexión SIN VERIFICAR.
7. Compatibilidad con lectores de pantalla: Narrator (soporta es-MX; las voces deben descargarse), NVDA y JAWS (los más usados según WebAIM).
8. Publicar el `.exe` en GitHub Releases con versión, tamaño y SHA-256, y enlazarlo desde la página de descargas del asistente.

## Herramientas
context7 (Compose Desktop, Gradle), code-review, OWASP Security Audit.

## Riesgos
Empaquetado `.exe` con JVM no confirmado; reputación de SmartScreen y falta de certificado de firma; accesibilidad de Compose Desktop con lectores de pantalla de Windows (SIN VERIFICAR); tiempos de certificación.

## Costos
Descarga por GitHub Releases: gratuita. Un certificado de firma de código tendría costo (SIN DECIDIR y SIN VERIFICAR el precio).

## Por decidir
Tecnología final tras la prueba; mismo nombre de la app; qué funciones móviles quedan fuera.
