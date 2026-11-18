# Reconocimiento de voz e integración de prueba

## Modo normal

El cliente conserva el contrato `POST /preguntar` (`pregunta`, `contexto`, `conversacionId`), la cabecera `X-Clave-App` y la respuesta `respuesta`, `proveedor`, `hayMas`, `conversacionId`. Se conserva la configuración existente de `local.properties`/variables de entorno al compilar. Para producción, el equipo debe confirmar una URL HTTPS y una clave de aplicación; la clave privada del proveedor de IA debe quedarse en el servidor, no en el instalador. No se configura ninguna IA comercial en este cambio.

## Modo de prueba (no IA)

En PowerShell, `$env:CECAPI_MODO_PRUEBA = '1'; .\gradlew.bat run`. El servidor HTTP embebido utiliza exclusivamente 127.0.0.1 y un puerto asignado por el sistema. La respuesta es fija y está marcada como simulada en la interfaz. No guarda audio ni conversaciones en disco, no llama a Internet y se cierra con la aplicación. Para volver al modo normal: `Remove-Item Env:CECAPI_MODO_PRUEBA -ErrorAction SilentlyContinue`. No publicar el modo de prueba como servicio real.

## Reconocimiento

En Windows, el botón Dictar pregunta abre una escucha de una frase mediante System.Speech. Se prefiere es-MX y, si no existe, otro reconocedor español. Se detiene con el botón, Esc o al cerrar la aplicación. El dictado completa el campo de pregunta, sin enviarlo automáticamente. El botón Comando de voz activa una gramática cerrada de instrucciones en español, separada del dictado libre. Los comandos exactos se interpretan localmente. Una confianza inferior a 0.60 rechaza el comando; el dictado utiliza 0.45. Estos umbrales son iniciales y requieren validación manual. Se requiere permiso de micrófono y un reconocedor instalado. El modo de prueba debe verificarse manualmente en Windows; el reconocimiento no se puede certificar mediante una compilación.

## Validación para la memoria

Capturar: Gradle test, pantalla con dictado, modo de prueba con respuesta simulada, ajuste por comando hablado, y archivo .exe generado. Probar errores de escucha, cancelar, voz y teclado. Verificar instalación y lanzamiento del .exe aparte. No afirmar pruebas con usuarios con discapacidad visual ni conexión a IA real sin evidencia.

## Retroalimentación audible
Esperar el tono agudo antes de hablar; el tono grave indica fin de captura. Las respuestas automáticas respetan la preferencia de lectura automática; las confirmaciones solicitadas son independientes. Verificar la primera lectura y los comandos en Windows; el dictado puede contener errores. El servidor local sigue siendo una simulación.

### Acceso por teclado
Con la ventana activa: F1 ayuda hablada, F2 dictado, F3 comandos, F4 lectura manual de la pregunta, F5 ajustes/volver, F6 lectura automática, F7 envío, Ctrl+R repetir, Escape cancelar/callar. Tab y Enter/espacio para recorrer/activar controles. Las confirmaciones de acciones solicitadas no dependen de la lectura automática. La lectura de respuestas respeta esa preferencia. Probar con lectura automática activada, volumen audible y las voces instaladas; no se afirma validación con lectores de pantalla o usuarios finales.

El botón de dictado recibe el foco inicial cuando el motor está disponible. Espacio activa o detiene ese botón; en el campo de texto escribe espacios y en otros controles conserva su función habitual. F2 sigue disponible como atajo global de la ventana.
