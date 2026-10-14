# Historial de cambios

Cada push a `main` compila y publica un release en
https://github.com/AbelG88/tolltube/releases

## Pendientes pedidos (en orden)

- [x] Ícono relacionado con YouTube (release 1)
- [x] Tiempo configurable en minutos y segundos (release 1)
- [ ] Sumas y restas con imágenes (emojis) acompañando los números
- [ ] Más tipos de ejercicio (animales, objetos) y elegirlos desde el menú

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
