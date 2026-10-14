# 🧠 Peaje Cognitivo - MVP

## Cómo obtener el APK sin instalar nada en tu computadora

### Opción A: GitHub Actions (recomendada, 100% gratis)

El repo es `AbelG88/tolltube`. Cada push a `main` compila solo; también se puede lanzar a mano.

1. **Ir a la pestaña "Actions"** del repo en GitHub
2. Si no arrancó solo, tocar **"Build APK"** y luego "Run workflow"
3. **Esperar ~3 minutos**
4. **Clickear el workflow más reciente → bajar a "Artifacts" → descargar `peaje-cognitivo-apk`**
5. El archivo `app-debug.apk` ya se puede instalar en el celular Android

### Opción B: Android Studio (si querés modificar algo)

1. Descargar Android Studio: https://developer.android.com/studio
2. Abrir este proyecto
3. Conectar el celular por USB (activar "Depuración USB" en Opciones de desarrollador)
4. Tocar el botón verde de "Play" ▶️

---

## Cómo instalar el APK en el celular del niño

1. Enviar el archivo `app-debug.apk` por WhatsApp, Telegram, Drive, o cable USB
2. En el celular, tocar el archivo
3. Si pide permiso: **Configuración → Permitir instalar de esta fuente**
4. Instalar

## Cómo usar la app

1. Abrir "Peaje Cognitivo"
2. Tocar "Permitir dibujar sobre otras apps" → activar el permiso
3. Tocar "Activar servicio de accesibilidad" → buscar "Peaje Cognitivo" → activar
4. Tocar "Probar overlay ahora" para ver si funciona
5. Abrir YouTube y esperar 1 minuto (en la versión de producción serán 10 min)

---

## Si el overlay a veces aparece y a veces no

La app escribe logs de diagnóstico con el tag `Peaje`. Con el celu conectado a la PC:

```
adb logcat -c
adb logcat -s Peaje:D > peaje-prueba-1.log
```

Después de reinstalar el APK hay que **apagar y volver a prender** el servicio de accesibilidad. En Android 13+ también hay que activar "Permitir ajustes restringidos" en Info de la app.

Hipótesis, protocolo de prueba y registro de resultados: [docs/diagnostico-intermitencia.md](docs/diagnostico-intermitencia.md).
Documento del proyecto: [docs/app-peaje-cognitivo.md](docs/app-peaje-cognitivo.md).

---

## Para cambiar el tiempo de espera

Editar `app/src/main/java/com/peajecognitivo/app/ExerciseTimerManager.kt`:
- `INTERVAL_MS = 60_000L` = 1 minuto (para pruebas)
- `INTERVAL_MS = 600_000L` = 10 minutos (para uso real)

## Para cambiar el ejercicio

Editar `app/src/main/java/com/peajecognitivo/app/PeajeOverlayManager.kt`:
- Cambiar la pregunta en `tvQuestion.text`
- Cambiar la respuesta correcta en `if (answer == "5")`
