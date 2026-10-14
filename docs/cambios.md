# Historial de cambios

Cada push a `main` compila y publica un release en
https://github.com/AbelG88/tolltube/releases

## Pendientes pedidos (en orden)

- [x] Ícono relacionado con YouTube (release 1)
- [x] Tiempo configurable en minutos y segundos (release 1)
- [x] Sumas y restas con imágenes (emojis) acompañando los números (release 2)
- [x] Más tipos de ejercicio (animales, objetos) y elegirlos desde el menú (release 2)

## Release 2: versión 1.2-MVP

Resultado de probar la 1.1: no apareció el overlay, aun con los permisos dados. Como la prueba se hace desde el celular (sin `adb`), este release suma herramientas de diagnóstico dentro de la app además de las funciones pedidas.

**Ejercicios**
- **Sumas y restas con dibujos.** Se muestran los números y, arriba, grupos de frutas con emojis grandes, uno por línea. Sumas de 1 a 5 + 1 a 5. Restas con minuendo de 2 a 6, nunca da 0 ni negativo. Se responde con teclado numérico, igual que antes.
- **Animales y objetos.** Un emoji grande y 3 botones con el nombre. Un botón equivocado se apaga y se puede probar con otro. Hay 24 animales y 27 objetos, con palabras rioplatenses (colectivo, zapatilla, lamparita, anteojos, torta).
- **Menú para elegir tipos.** Casillas en la pantalla principal. Si hay varios tildados salen mezclados, y siempre queda al menos uno. No repite el mismo ejercicio dos veces seguidas. Por defecto están los cuatro.
- El overlay ahora tiene **scroll**, así que ya no se corta en horizontal (mitiga H6).

**Diagnóstico desde el celular**
- **Panel de estado** en la pantalla principal, en vivo: permiso de overlay, accesibilidad activada en Ajustes, servicio conectado, última app detectada y si se detecta YouTube.
- **📋 Ver registro.** Pantalla con el log de la app, con botones para copiar, compartir, actualizar y borrar. El log se guarda en un archivo y sobrevive si el proceso muere.
- **Captura de crashes.** Si la app se cae, el motivo queda en el registro (línea `CRASH`).
- **Línea `PROCESO iniciado`** cada vez que arranca el proceso, para detectar reinicios.
- Se agregaron `com.google.android.apps.youtube.kids` y `com.google.android.apps.youtube.mango` (YouTube Go) a los paquetes que cuentan como YouTube.

## Release 1: versión 1.1-MVP

- **Ícono nuevo.** Original: botón de play + barrera de peaje, en vector (adaptive icon). No usa el logo de YouTube, que es marca registrada.
- **Tiempo configurable.** Pantalla principal con minutos y segundos. Guardado en el celular (`SettingsStore`). Mínimo 10 s, máximo 3 h, por defecto 60 s.
- **Temporizador interno.** Se reemplazó `AlarmManager.setRepeating` por un `Handler`. Motivo: Android no permite alarmas repetidas de menos de 60 s y además son inexactas. Esto mitiga la hipótesis H4 de [diagnostico-intermitencia.md](diagnostico-intermitencia.md). Las hipótesis H1, H2 y H3 siguen sin tocar, a propósito, hasta tener logs.
- **Firma fija de debug.** `app/debug.keystore`. Desde este build, cada APK nuevo se instala encima del anterior. Antes, cada build de GitHub se firmaba con una clave distinta y obligaba a desinstalar.
- **Versión visible** en la pantalla principal.
- Se quitó `ExerciseAlarmReceiver` y su entrada en el manifest (ya no se usa).

## Build #15: versión 1.0-MVP + logs

- Logs de diagnóstico con el tag `Peaje`.
- Documentación en `docs/`.
- El APK se publica como Release en cada build.
