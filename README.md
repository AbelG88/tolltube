# 🧠 Peaje Cognitivo - MVP

## Cómo obtener el APK sin instalar nada en tu computadora

### Opción A: GitHub Actions (recomendada, 100% gratis)

El repo es `AbelG88/tolltube`. Cada push a `main` compila solo y deja el APK en **Releases** (https://github.com/AbelG88/tolltube/releases/latest). Desde el celu, bajá `peaje-cognitivo-debug.apk` de ahí: es la forma más fácil. Lo de abajo (Artifacts) es el camino alternativo.

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
5. Elegir el tiempo entre ejercicios (por defecto 1 minuto) y tocar "Guardar tiempo"
6. Abrir YouTube y esperar ese tiempo

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

## Para cambiar el tiempo entre ejercicios

Desde la app: pantalla principal → **⏱️ Tiempo entre ejercicios** → minutos y segundos → **Guardar tiempo**. Mínimo 10 segundos, máximo 3 horas. Por defecto es 1 minuto. Si ya está corriendo un timer, se reprograma al guardar.

## Instalar una versión nueva encima de otra

Desde el Build #16 en adelante todos los APK van firmados con la misma clave (`app/debug.keystore`), así que se instalan **encima** del anterior sin desinstalar. La primera vez, si tenías instalado el Build #15 o anterior, hay que **desinstalarlo** (esa versión se firmó con otra clave). Después de actualizar, apagá y prendé el servicio de accesibilidad.

El historial de versiones está en [docs/cambios.md](docs/cambios.md).

## Para cambiar el ejercicio

Editar `app/src/main/java/com/peajecognitivo/app/PeajeOverlayManager.kt`:
- Cambiar la pregunta en `tvQuestion.text`
- Cambiar la respuesta correcta en `if (answer == "5")`
