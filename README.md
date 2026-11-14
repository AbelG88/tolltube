# 🧠 Tranquera

App Android de control parental con un giro educativo: cuando el niño usa una app de las que cuidamos (YouTube, TikTok, juegos…), cada cierto tiempo aparece un **ejercicio** que tiene que resolver para seguir. No bloquea: convierte el tiempo de pantalla en práctica.

Versión actual: **3.6.0-MVP** (versionCode 28) · Paquete: `com.tranquera.app` · Licencia: ver [LICENSE](LICENSE).

## Qué hace

- **Ejercicios en pantalla** sobre la app que usa el niño: sumas, restas, animales, objetos, emociones, letras, colores, formas, secuencias, etc.
- **Varios niños:** un panel por niño; el padre elige el activo. El nivel sale de la edad.
- **Asistente de 4 pasos:** nombre y edad → qué le gusta → qué apps cuidamos → permisos y PIN.
- **Qué apps cuidamos:** las de siempre (YouTube, YouTube Kids, TikTok, Instagram, Netflix), las que elijas de la lista de instaladas, o todo menos lo que permitas.
- **Modos:** Suave (cada 20 min), Normal (cada 10 min) e Intenso (2 ejercicios cada 5 min). Tiempo ajustable desde 10 segundos hasta 3 horas.
- **Módulos de ejercicios en JSON** (`/modulos`), incluidos en el APK. Hay una categoría propia en Ajustes para activarlos, importarlos o **crear el tuyo desde la app** (preguntas con su respuesta correcta) y exportarlo como `.json`.
- **Refuerzos:** voz que lee el ejercicio, ayuda tras errores, estrellas y contador diario, progreso por tipo.
- **Seguridad:** PIN de padres guardado con hash, bloqueo por intentos fallidos, registro sin datos sensibles.
- **Apoyo voluntario:** pedido solo en el panel de padres, cada 100 aciertos (nunca sobre las apps del niño).

## Stack

- Kotlin + Android (AGP 8.2.0, Kotlin 1.9.20, compileSdk/targetSdk 34, minSdk 26), vistas XML con Material. Sin Compose.
- Dependencias mínimas: `core-ktx`, `appcompat`, `material`, `constraintlayout`.
- Detección de la app en primer plano solo con `UsageStatsManager` (datos de uso); **no usa servicio de accesibilidad**. Overlay con `SYSTEM_ALERT_WINDOW`.
- Tipografía Quicksand (licencia OFL en [licencias/](licencias/)).

Estructura:

```
app/src/main/java/com/tranquera/app/   código (MainActivity, AsistenteActivity, TranqueraOverlayManager, SettingsStore…)
modulos/                                    módulos de ejercicios (JSON), empaquetados como assets
docs/                                       documento del proyecto, cambios, Play Store, diagnóstico, privacidad
.github/workflows/build.yml                 compilación automática
```

## Cómo obtener el APK

### Opción A: GitHub Actions (sin instalar nada)

Cada push a `main` compila solo y publica el APK en **Releases**: https://github.com/AbelG88/tolltube/releases/latest. Desde el celu bajá `tranquera.apk`.

También se puede disparar a mano: pestaña **Actions → Build APK → Run workflow** (~3 min) y bajar `tranquera-apk` de *Artifacts*.

El workflow además verifica que compile la variante `release` (sin registro de diagnóstico) y, si están cargados los secrets de firma, arma el `.aab` para Google Play.

### Opción B: compilar en tu máquina

Requisitos: JDK 17 y el Android SDK (el wrapper `./gradlew` baja Gradle 8.2 solo).

```
./gradlew testDebugUnitTest   # tests unitarios
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
./gradlew assembleRelease     # verificación de la variante release
```

O abrí el proyecto en Android Studio, conectá el celu con "Depuración USB" y tocá ▶️.

## Instalación y primer uso

1. Pasá el APK al celu del niño (WhatsApp, Drive, cable) y abrilo. Si lo pide: *Permitir instalar de esta fuente*.
2. Abrí **Tranquera** y seguí el asistente.
3. Otorgá los permisos que pide la app:
   - **Mostrar sobre otras apps** (overlay).
   - **Acceso a datos de uso** (así se detecta qué app está abierta).
   - En Android 13+: *Info de la app → ⋮ → Permitir ajustes restringidos* (la app tiene una guía paso a paso).
4. Definí el **PIN** de padres.
5. Tocá **Probar ahora** y abrí una de las apps cuidadas.

**Actualizar:** desde el Build #16 todos los APK van firmados con la misma clave (`app/debug.keystore`), así que se instalan encima del anterior. Si venías del #15 o anterior, desinstalá una vez.

## Personalizar

- **Tipos de ejercicio y tiempo:** desde la app (Ajustes → Avanzado) o editando `ExerciseGenerator.kt` (pares `"emoji" to "Nombre"`).
- **Módulos de contenido:** agregá un `.json` en `modulos/` o importalo desde la app. Formato mínimo:

  ```json
  {
    "formato": 1,
    "nombre": "Animales",
    "descripcion": "Reconocer animales",
    "edad": "3 a 7 años",
    "ejercicios": [
      { "tipo": "opciones", "visual": "🐶", "pregunta": "¿Qué animal es?",
        "correcta": "Perro", "incorrectas": ["Conejo", "Cerdo", "Tortuga"] }
    ]
  }
  ```

  Ejemplo completo en [docs/modulos-ejemplo/dinosaurios.json](docs/modulos-ejemplo/dinosaurios.json).

## Si el overlay a veces aparece y a veces no

- **Desde el celu:** panel **🔎 Estado** → **📋 Ver registro** → Copiar o Compartir. Muestra permisos, estado del servicio y la última app detectada.
- **Con PC:** logs con el tag `Tranquera`:

  ```
  adb logcat -c
  adb logcat -s Tranquera:D > tranquera-prueba-1.log
  ```

- Algunos fabricantes (Samsung, Xiaomi, Motorola…) matan procesos en segundo plano: excluí la app de la optimización de batería.
- Hipótesis y protocolo de prueba: [docs/diagnostico-intermitencia.md](docs/diagnostico-intermitencia.md).

## Estado y hoja de ruta

El proyecto es un **MVP en pruebas reales**; las compilaciones se verifican por CI y la prueba en el celu es manual. Hay tests unitarios del parser de módulos (incluida la validación de todos los `modulos/*.json`) que corren en CI; falta cubrir PIN y generador de ejercicios.

Pendientes principales (detalle en [docs/ideas-pendientes.md](docs/ideas-pendientes.md) y [docs/camino-a-play-store.md](docs/camino-a-play-store.md)):

- Resolver la intermitencia del overlay (H3, H5 y H7 sin observar).
- Clave de firma propia, política de privacidad pública y formulario de seguridad de datos.
- Campo obligatorio de compatibilidad en los módulos importables.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/app-tranquera.md](docs/app-tranquera.md) | Idea, objetivos y fases del proyecto |
| [docs/cambios.md](docs/cambios.md) | Historial de versiones |
| [docs/ideas-pendientes.md](docs/ideas-pendientes.md) | Ideas y pendientes |
| [docs/camino-a-play-store.md](docs/camino-a-play-store.md) | Requisitos para publicar en Google Play |
| [docs/diagnostico-intermitencia.md](docs/diagnostico-intermitencia.md) | Diagnóstico del overlay intermitente |
| [docs/politica-de-privacidad.md](docs/politica-de-privacidad.md) | Política de privacidad |
| [docs/play-seguridad-de-datos.md](docs/play-seguridad-de-datos.md) | Declaración de seguridad de datos |
