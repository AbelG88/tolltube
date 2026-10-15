# Historial de cambios

Cada push a `main` compila y publica un release en
https://github.com/AbelG88/tolltube/releases

## Pendientes pedidos (en orden)

- [x] Ícono relacionado con YouTube (release 1)
- [x] Tiempo configurable en minutos y segundos (release 1)
- [x] Sumas y restas con imágenes (emojis) acompañando los números (release 2)
- [x] Más tipos de ejercicio (animales, objetos) y elegirlos desde el menú (release 2)

## Release 6: versión 1.6-MVP

- **Cinco tipos nuevos:** 🔤 Letras (nivel fácil: reconocer la letra; medio y difícil: con qué letra empieza el animal), 🎨 Colores (corazones de colores), 🔷 Formas, 🔢 Contar (dibujos repetidos, respuesta con teclado) y ➡️ Secuencias de números (qué número sigue). Hay que tildarlos en el menú.
- **Dificultad:** Fácil / Medio (por defecto, igual que antes) / Difícil. Cambia los rangos de sumas, restas, contar y secuencias, y el modo de Letras.
- **Progreso por tipo:** ejercicios resueltos y errores acumulados, en la pantalla principal (detrás del PIN), con botón para borrarlo. Se guarda solo en el celu.
- **APK sin la palabra "debug":** el archivo del Release ahora es `peaje-cognitivo.apk` y el título dice "Peaje Cognitivo - build #N". Por dentro sigue siendo el mismo tipo de compilación y la misma firma, así que se instala encima de la versión anterior.

## Release 5: versión 1.5-MVP

- **Orientación en el registro:** la línea `OVERLAY show()` ahora dice si la pantalla estaba horizontal o vertical.
- **Estrellas del día:** al resolver bien, el cartelito muestra ⭐ y cuántos ejercicios van hoy; el panel de estado también. Se reinicia cada día. Sin castigos ni puntos negativos.

Pendiente de [ideas-pendientes.md](ideas-pendientes.md): aviso previo suave, más tipos de ejercicio y niveles.

## Release 4.2: versión 1.4.2-MVP

- **El video se pausa cuando aparece el ejercicio.** Se pide el foco de audio (YouTube se pausa solo) y se manda la tecla "pausa" de medios. Nunca manda "play": al terminar, el video queda pausado o se reanuda según lo haga YouTube. Sirve también para escuchar la voz. En el registro: líneas `AUDIO ...`.

## Release 4.1: versión 1.4.1-MVP

Arreglos tras probar la 1.4:

- **Botón OK al lado del campo de respuesta** en sumas y restas (antes quedaba debajo y el teclado lo tapaba).
- **Versión en el registro:** primera línea de la pantalla de registro (y de lo que se copia o comparte) y línea `PROCESO versión=...` cada vez que arranca la app.
- **PIN:** no se pedía porque se pedía solo al crear la pantalla y quedaba desbloqueado al guardarlo. Ahora se pide cada vez que se vuelve a la app, y vale 1 minuto después de ingresarlo. Al poner un PIN nuevo no queda desbloqueado.

## Release 4: versión 1.4-MVP

Ideas y próximos pasos: [ideas-pendientes.md](ideas-pendientes.md).

- **Emociones:** nuevo tipo "¿Cómo se siente?" con caras (feliz, triste, enojado, asustado, sorprendido, con sueño). Si venías con tipos guardados de antes, hay que tildarlo en el menú.
- **Voz:** casilla "Leer el ejercicio en voz alta" (apagada por defecto). Usa el motor de voz del sistema, en español.
- **Ayuda tras errores:** en opciones, tras el primer error se resalta en verde la correcta; en sumas y restas, tras dos errores aparece la respuesta como pista.
- **PIN de padres:** botón para poner, cambiar o quitar un PIN de 4 a 6 números. Protege la pantalla de ajustes y el registro (queda desbloqueado 5 minutos).

## Release 3: versión 1.3-MVP

Arreglo de H1 (ver [diagnostico-intermitencia.md](diagnostico-intermitencia.md)), con la evidencia del registro de la 1.2: el teclado, la barra del sistema y el propio overlay apagaban el timer.

- **Los eventos ya no apagan el timer.** Solo lo arrancan cuando aparece YouTube.
- **En cada ciclo se pregunta qué app está realmente en pantalla** (`rootInActiveWindow`):
  - YouTube → se muestra el ejercicio.
  - Otra app (launcher, WhatsApp, etc.) → el timer se detiene y se reactiva solo al volver a YouTube.
  - Incierto (teclado, barra del sistema, nuestro overlay, o no se pudo leer) → se saltea el ciclo y se sigue contando.
- **El tiempo del siguiente ejercicio se cuenta desde que se resolvió el anterior.** Antes pasaba así por casualidad (el overlay apagaba el timer y YouTube lo rearmaba al cerrarse). Ahora es explícito.
- **Al conectar el servicio se mira la pantalla** (a los 0 y a los 1,5 s). Cubre el caso de YouTube ya abierto antes de activar la accesibilidad (H2).
- **Registro más limpio.** Las ventanas de ruido repetidas se anotan una sola vez seguida. Las líneas `TIMER sonó` ahora dicen `en_pantalla=<paquete> -> YOUTUBE / OTRA_APP / INCIERTO`.
- **Panel de estado:** nueva línea "Timer" (corriendo con cuenta regresiva, esperando el ejercicio, o detenido).

Qué probar con la 1.3: ver la sección "Qué probar con la 1.3" en el diagnóstico.

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
