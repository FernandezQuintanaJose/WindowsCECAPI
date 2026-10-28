# Ficha: App Windows (WindowsCECAPI)

Responsable: Alonso. Complementa la ficha global (`00_global`). El repositorio está vacío.

## Objetivo
La misma app móvil llevada a Windows, ocultando o desactivando lo que no se pueda hacer.

## Tecnología candidata
Kotlin con Compose Desktop (genera MSI, EXE, DEB). Conversión a MSIX para la Microsoft Store: SIN VERIFICAR para apps JVM, es el primer experimento. Reutilizar la lógica común con la app Android donde sea posible.

## Distribución
- Principal: Microsoft Store (firma y hospedaje gratuitos; cuenta individual gratuita con ID y selfie; certificación de hasta 3 días hábiles). Cuenta a nombre de José Fernández Quintana.
- Respaldo: descarga desde GitHub Releases (puede dar alertas de SmartScreen por falta de reputación) y Linux (DEB).
- Evitar empaquetar con PyInstaller por falsos positivos de antivirus.

## Tareas, en orden
1. Prueba técnica: app mínima en Compose Desktop empaquetada como MSIX e instalada en un equipo limpio. Resultado: sí/no.
2. `FUNCIONES.md`: tabla de funciones de la app móvil (por módulo) con estado en Windows: igual, adaptada, oculta.
3. Detección de capacidades para ocultar lo no soportado.
4. Cliente de la API con inicio de sesión de Google y token.
5. Pantallas de políticas con audio.
6. Voz: reconocimiento y síntesis en español; sin conexión SIN VERIFICAR.
7. Compatibilidad con lectores de pantalla: Narrator (soporta es-MX; las voces deben descargarse), NVDA y JAWS (los más usados según WebAIM).
8. Envío a la Store con cumplimiento de políticas 10.5.x, 11.11 y 11.16.

## Herramientas
context7 (Compose Desktop, Gradle), code-review, OWASP Security Audit.

## Riesgos
MSIX con JVM no confirmado; reputación de SmartScreen; accesibilidad de Compose Desktop con lectores de pantalla de Windows (SIN VERIFICAR); tiempos de certificación.

## Costos
Cuenta individual de la Store gratuita; sin certificado propio si se publica por la Store.

## Por decidir
Tecnología final tras la prueba; mismo nombre de la app; qué funciones móviles quedan fuera.
