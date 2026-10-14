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

### H1: el timer se cancela por eventos de otras ventanas (la más probable)

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

### H6: landscape

`overlay_exercise.xml` no tiene scroll y usa paddings y tamaños fijos. En horizontal la altura no alcanza y se corta. Es un bug de layout aparte, no de intermitencia.

## Cómo capturar logs

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

## Tabla "cuándo sí / cuándo no"

Predicción inicial según el código. Se corrige con los logs.

| Escenario | Predicción | Resultado real | Hipótesis |
|-----------|------------|----------------|-----------|
| A | Funciona | _pendiente_ | |
| B | Falla en el ciclo siguiente | _pendiente_ | H1 |
| C | Falla | _pendiente_ | H1 |
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

## Después del diagnóstico

Cuando los logs confirmen cuáles hipótesis son reales, el arreglo probable es:

- **H1:** ignorar eventos de teclado, SystemUI y del propio paquete. Solo cancelar el timer cuando el paquete en primer plano sea una app real distinta de YouTube.
- **H2:** al conectar el servicio, consultar la ventana activa (`rootInActiveWindow`) para inicializar `isYouTubeActive`.
- **H3:** asignar `overlayView` solo después de que `addView` tenga éxito.
- **H4:** pasar a un temporizador propio mientras YouTube está activo, en vez de `setRepeating`.
- **H5:** pedir exclusión de optimización de batería y documentar los pasos por marca.
- **H6:** envolver el layout en un `ScrollView` y reducir tamaños en horizontal.
