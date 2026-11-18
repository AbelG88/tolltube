# Declaraciones de permisos para Play Console

Textos listos para pegar en Play Console (Contenido de la app). Revisalos antes de enviar.

## Acceso al uso de apps (`PACKAGE_USAGE_STATS`)
**Para qué:** saber qué app está abierta en primer plano, para pedir un ejercicio antes de seguir en las apps que el padre eligió cuidar (por ejemplo YouTube).
**Qué no hace:** no envía nada a internet, no guarda historial de uso, no lee el contenido de las apps. Todo queda en el celular.
**Dónde se pide:** el padre lo activa en Ajustes de Android durante el asistente de primera vez, con una pantalla que lo explica antes.

## Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`)
**Para qué:** mostrar el ejercicio encima de la app cuidada. Es la función central: sin esto el ejercicio no aparece.
**Cómo se usa:** solo aparece el ejercicio, sin anuncios ni contenido ajeno. Se cierra al resolverlo.
**Dónde se pide:** en el asistente, con explicación previa.

## Servicio en primer plano (`FOREGROUND_SERVICE_SPECIAL_USE`)
**Subtipo (texto para el manifest/consola):** "Vigila qué app está abierta para pedir ejercicios educativos como control parental".
**Por qué no puede ser otro tipo:** no es multimedia, ubicación ni dispositivo conectado; es un monitor continuo elegido por el padre, con notificación visible y acción "Abrir (PIN)".

## Video demostrativo (si Play lo pide)
Grabar en el celu: asistente → permisos → abrir YouTube → aparece el ejercicio → resolverlo → sigue YouTube. Mostrar la notificación del servicio.

## Resumen para la ficha (sin promesas terapéuticas)
- Público objetivo: adultos (madres, padres, tutores).
- Sin anuncios, sin cuentas, sin analíticas ni rastreadores.
- Los datos (nombre, edad, progreso) se guardan solo en el celular; ver `politica-de-privacidad.md` y `play-seguridad-de-datos.md`.
