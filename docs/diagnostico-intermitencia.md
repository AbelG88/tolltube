# Diagnóstico: por qué el overlay a veces aparece y a veces no

Estado: **hipótesis a verificar en el celular**. Escrito a partir de leer el código (commit `b723dbc`), todavía sin logs reales. Cada hipótesis dice qué línea del log la confirma o la descarta.

## Síntoma

- A veces el overlay aparece sobre YouTube a los ~1 minuto. Otras veces no aparece nunca.
- Después de recompilar e instalar, a veces deja de disparar del todo.
- En horizontal (landscape) el contenido del overlay se corta.

## Cómo funciona hoy (resumen)

1. `PeajeAccessibilityService` escucha `TYPE_WINDOW_STATE_CHANGED` de **todas** las apps.
2. Si el paquete es YouTube, marca `isYouTubeActive = true` y llama a `ExerciseTimerManager.startTimer()`. Si es cualquier otro paquete, marca `false` y cancela el timer.
3. `startTimer()` programa una alarma `setRepeating` (inexacta) cada 60 s.
4. Cuando suena, `ExerciseAlarmReceiver` muestra el overlay solo si `isYouTubeActive` es `true` en ese instante.
5. `PeajeOverlayManager.show()` agrega una vista `TYPE_APPLICATION_OVERLAY` a pantalla completa.

## Hipótesis

**Estado después de la prueba con registro del 14/10/2026 (versión 1.2, Motorola):**

| Hipótesis | Estado |
|-----------|--------|
| H1 | **Confirmada.** Ver "Resultados de la prueba con registro" más abajo |
| H2 | No se observó. Falta probar el escenario F (YouTube ya abierto al activar el servicio) |
| H3 | No se observó: `addView OK` las 3 veces |
| H4 | **Mitigada.** Con el timer interno el retraso fue de 2 a 29 ms |
| H5 | No se observó en ~15 minutos de registro |
| H6 | Sin probar en horizontal con la 1.2 (el overlay ahora tiene scroll) |
| H7 | No se observó: no hay líneas `CRASH` |
| H8 | Descartada para YouTube normal: el paquete es `com.google.android.youtube` |

### H1: el timer se cancela por eventos de otras ventanas (confirmada el 14/10)

Cualquier cambio de ventana de otro paquete cuenta como "salió de YouTube": el teclado (al tocar el campo de respuesta), la barra de notificaciones, SystemUI, el volumen, el propio overlay. Eso cancela la alarma. Al volver a YouTube **no siempre** llega un evento nuevo, así que el timer queda cancelado hasta salir y volver a entrar.

- Se confirma si en el log aparece `salió de YouTube (por pkg=<algo que no es YouTube>)` + `TIMER cancelado`, y después no hay ningún `entró a YouTube`.
- Candidatos a ver en `pkg=`: `com.android.systemui`, el paquete del teclado, `com.peajecognitivo.app`.

### H2: servicio desvinculado o estado en memoria perdido (típico tras recompilar)

- Al reinstalar el APK, Android desvincula el servicio de accesibilidad. Hay que apagarlo y prenderlo en Ajustes.
- En Android 13+, un APK instalado a mano necesita **Ajustes → Apps → Peaje Cognitivo → ⋮ → Permitir ajustes restringidos** antes de poder activar la accesibilidad.
- `isYouTubeActive` vive en memoria. Si el servicio reinicia con YouTube ya abierto, no llega ningún evento, queda en `false` y las alarmas se ignoran.
- Se confirma si no aparece `SERVICIO conectado` después de instalar, o si aparece `ALARMA sonó. isYouTubeActive=false` estando YouTube a la vista.

### H3: overlay trabado por `addView` fallido

`overlayView` se asigna **antes** de `addView`. Si `addView` lanza una excepción (permiso de overlay revocado, por ejemplo), queda no-nulo y `show()` se ignora en silencio hasta que muera el proceso.

- Se confirma con `OVERLAY addView FALLÓ` seguido de muchos `OVERLAY show() ignorado`.

### H4: alarma inexacta y retrasada por ahorro de batería

`setRepeating` es inexacta desde Android 4.4. Doze, el ahorro de batería y el agrupamiento de alarmas pueden retrasarla mucho más de 60 s. Si suena cuando hay otra ventana arriba, se saltea y no reintenta hasta el próximo ciclo.

- Se confirma con `retraso_vs_esperada` grande (más de unos segundos) en las líneas `ALARMA sonó`.
- **Mitigada desde la versión 1.1-MVP:** el timer ahora es un `Handler` interno. Las líneas del log pasan a decir `TIMER sonó` y `retraso_vs_esperado`. Los logs del Build #15 (alarma) y los del 1.1 (Handler) no son comparables entre sí para esta hipótesis.

### H5: el fabricante mata el servicio o el proceso

Samsung, Xiaomi, Motorola y otros matan procesos en segundo plano y a veces desactivan el servicio de accesibilidad. No hay servicio en primer plano que lo proteja.

- Se confirma con `SERVICIO destruido` o `desvinculado` sin que nadie haya tocado Ajustes, o con el contador `+Nms` volviendo a valores chicos (el proceso reinició).

### H7: el proceso se cae y el servicio de accesibilidad no vuelve

Nueva desde la prueba de la 1.1 (no apareció nada). `PeajeOverlayManager.show()` vuelve a lanzar la excepción si `addView` falla, y eso se ejecuta en el hilo principal. Una excepción sin capturar mata el proceso. Cuando un servicio de accesibilidad se cae así, Android no siempre lo vuelve a conectar hasta que el usuario lo apague y lo prenda.

- Se confirma con una línea `CRASH en hilo ...` en el registro, seguida de `PROCESO iniciado` con un pid nuevo, y con el panel de estado mostrando "Servicio conectado y vivo" en ❌ aunque la accesibilidad figure activada.

### H8: el paquete de la app de video no está en la lista

La detección solo reconoce un conjunto fijo de paquetes de YouTube. Si en el celu se usa otra variante, nunca se activa el timer.

- Se confirma mirando "Última app detectada" en el panel de estado mientras YouTube está abierto: si muestra un paquete que no es de la lista, ese es el problema. Desde la 1.2 se agregaron YouTube Kids y YouTube Go.

### H6: landscape

`overlay_exercise.xml` no tiene scroll y usa paddings y tamaños fijos. En horizontal la altura no alcanza y se corta. Es un bug de layout aparte, no de intermitencia.

## Cómo capturar logs

### Desde el celular, sin PC (desde la versión 1.2)

1. Abrir la app y mirar el **🔎 Estado** de la pantalla principal. Con YouTube abierto en otra pantalla, volver a la app y ver "Última app detectada" y "Servicio conectado y vivo".
2. Hacer el escenario de prueba.
3. Volver a la app → **📋 Ver registro** → **Copiar** (o **Compartir**) y pegar el texto en el chat.
4. Entre pruebas, **Borrar** para empezar con un registro limpio.

### Con PC y adb

Con el celu conectado por USB (o depuración inalámbrica) y `adb` en la PC:

```
adb devices
adb install -r app-debug.apk
adb logcat -c
adb logcat -s Peaje:D > peaje-prueba-1.log
```

Todo lo que loguea la app sale con el tag `Peaje`. Cada línea lleva `[+Nms]`, el tiempo desde que arrancó el proceso.

Comandos útiles para completar el cuadro:

```
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys alarm | grep -i peaje
```

El primero muestra si el servicio de accesibilidad figura habilitado. El segundo muestra si hay una alarma viva de la app.

## Protocolo de prueba

Hacer cada escenario con una captura de log nueva y anotar el resultado en la tabla de abajo.

| # | Escenario | Qué hacer |
|---|-----------|-----------|
| A | Camino feliz | Servicio recién activado, abrir YouTube, no tocar nada 90 s |
| B | Teclado | Igual que A, pero tocar el campo de respuesta y contestar bien. Esperar el siguiente ciclo |
| C | Barra de notificaciones | En YouTube, bajar y subir la barra. Esperar 90 s |
| D | Después de instalar | Reinstalar el APK, **sin** tocar Ajustes, abrir YouTube |
| E | Después de reinstalar + reactivar | Igual que D, pero apagando y prendiendo el servicio |
| F | YouTube ya abierto | Abrir YouTube, **después** activar el servicio, volver a YouTube |
| G | Pantalla apagada y vuelta | Bloquear 2 min, desbloquear, volver a YouTube |
| H | Horizontal | Overlay en landscape |

### Datos ya conocidos (sin registro todavía)

| Fecha | Versión | Dato |
|-------|---------|------|
| 14/10/2026 | 1.1 | Con los permisos dados, **no apareció el overlay** en una primera prueba. No se pudo ver el registro (recién existe en la 1.2). |
| 14/10/2026 | 1.1 | **Si se maximiza YouTube a pantalla completa apaisada, el overlay sí aparece. En vertical (normal), no.** |

**Lectura del segundo dato.** Es coherente con H1 y H2: el timer no estaba corriendo en vertical porque `isYouTubeActive` había quedado en `false` (otra ventana lo apagó, o el servicio arrancó con YouTube ya abierto). Al pasar a pantalla completa, YouTube cambia de ventana, genera un evento nuevo, el servicio vuelve a marcar `true` y arranca el timer. Si es así, el overlay no falla al dibujarse: falla la detección de que YouTube está en primer plano.

Para confirmarlo con la 1.2: en vertical con YouTube reproduciendo, volver a la app y mirar el panel **🔎 Estado** ("YouTube detectado ahora" debería decir **no**), y en el registro buscar el evento de YouTube que aparece justo al maximizar.

## Tabla "cuándo sí / cuándo no"

Predicción inicial según el código. Se corrige con los logs.

| Escenario | Predicción | Resultado real | Hipótesis |
|-----------|------------|----------------|-----------|
| A | Funciona | Funcionó: overlay a los 30 s, con 2 ms de retraso | |
| B | Falla en el ciclo siguiente | El timer se cancela cuando se cierra el teclado. Mecanismo confirmado en el log; falta confirmar que el usuario seguía en YouTube | H1 |
| C | Falla | El timer se cancela, y se recupera solo si YouTube emite un evento después. Pasó las 2 veces que siguió en YouTube | H1 |
| D | Falla | _pendiente_ | H2 |
| E | Funciona | _pendiente_ | H2 |
| F | Falla | _pendiente_ | H2 |
| G | Incierto | _pendiente_ | H4, H5 |
| H | Se corta | _pendiente_ | H6 |

## Registro de pruebas

Una fila por prueba real.

| Fecha | Dispositivo / Android | Escenario | Resultado | Archivo de log | Notas |
|-------|-----------------------|-----------|-----------|----------------|-------|
| | | | | | |

## Resultados de la prueba con registro (versión 1.2)

Celular Motorola (launcher `com.motorola.launcher3`), YouTube en vertical, timer en 30 s, 14/10/2026 entre las 20:45 y las 21:00. El log completo no se subió al repo porque es público y muestra qué apps se usaron; abajo va solo lo relevante.

**Lo que funcionó.** Tres overlays, todos a tiempo: a las 20:47:01, 20:47:38 y 20:48:14. Retraso del timer: 2, 29 y 25 ms. Los ejercicios de objetos y animales (con botones) se cerraron bien y el ciclo siguió.

**Lo que muestra el log sobre la fragilidad (H1).** Hay tres cosas que apagan el timer, y el servicio lo trata como "salió de YouTube":

1. **Barra del sistema.** `com.android.systemui` cancela el timer (20:46:29, 20:46:31 y 20:50:53). Las dos primeras veces YouTube emitió un evento nuevo en menos de 1,5 s y el timer volvió a armarse. Eso no está garantizado.
2. **El propio overlay.** Cada vez que aparece, la ventana del overlay (`com.peajecognitivo.app`, `ScrollView`) cancela el timer (20:47:02, 20:47:39, 20:48:14). El ciclo continúa solo porque YouTube emite un evento cuando el overlay se cierra (20:47:08, 20:47:44, 20:48:29). Funciona por suerte, no por diseño.
3. **El teclado.** En la suma de las 20:48, YouTube rearmó el timer a las 20:48:29.099 y el teclado (`com.google.android.inputmethod.latin`) lo canceló 0,5 s después, al cerrarse. Después no hubo ningún evento de YouTube hasta las 20:50:51, así que si el usuario siguió viendo YouTube en ese rato, el overlay no iba a salir. Es el mismo mecanismo que explica el "a veces sí, a veces no": los ejercicios con botones no abren teclado y el ciclo sigue; los numéricos sí, y el timer puede morir.

```
20:48:29.098 EVENTO pkg=com.google.android.youtube ... false -> true
20:48:29.099 -> entró a YouTube: startTimer
20:48:29.585 EVENTO pkg=com.google.android.inputmethod.latin ... true -> false
20:48:29.587 -> salió de YouTube (por pkg=com.google.android.inputmethod.latin): pauseTimer
20:48:29.588 TIMER cancelado
```

**Pregunta abierta.** ¿El usuario seguía en YouTube después de la suma 3+5 de las 20:48? Si sí, ahí hubo una falla real.

## Después del diagnóstico

El arreglo para H1, ya con evidencia:

- **H1:** dejar de apagar el timer por eventos. El timer corre mientras haya un evento de YouTube reciente y, **en cada ciclo, se consulta cuál es la app realmente en pantalla** (`rootInActiveWindow`) en lugar de fiarse de `isYouTubeActive`. Así el teclado, la barra del sistema y el propio overlay dejan de importar.

Para el resto, si aparecen:

- **H2:** al conectar el servicio, consultar la ventana activa (`rootInActiveWindow`) para inicializar `isYouTubeActive`.
- **H3:** asignar `overlayView` solo después de que `addView` tenga éxito.
- **H4:** pasar a un temporizador propio mientras YouTube está activo, en vez de `setRepeating`. *Hecho en la 1.1.*
- **H5:** pedir exclusión de optimización de batería y documentar los pasos por marca.
- **H6:** envolver el layout en un `ScrollView` y reducir tamaños en horizontal. *Hecho en la 1.2 (falta probarlo en horizontal).*
