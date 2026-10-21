# Respuestas sugeridas — Seguridad de los datos (Play Console)

Referencia para completar el formulario. Verificar en pantalla, porque Google cambia las preguntas.

- ¿La app recopila o comparte datos de usuario? → **No.** Nada sale del teléfono (sin permiso de Internet, sin SDK de terceros, sin copias de seguridad en la nube).
- Cifrado en tránsito → no aplica (no hay transmisión).
- Pedido de eliminación de datos → no aplica; se borra al desinstalar.
- Cuenta de usuario → no hay.
- Política de privacidad (URL obligatoria) → publicar `docs/politica-de-privacidad.md` (p. ej. con GitHub Pages) y pegar la URL.

## Otras declaraciones pendientes
- **Público objetivo y contenido:** la app se usa con niños → revisar la política de Familias.
- **Permiso "Acceso a datos de uso" (PACKAGE_USAGE_STATS):** explicar en el formulario que se usa solo para detectar YouTube en primer plano (función principal).
- **Servicio en primer plano `specialUse`:** declarar el motivo "parental_control_foreground_app_monitor".
- **Mostrar sobre otras apps (SYSTEM_ALERT_WINDOW):** es la función principal (el ejercicio sobre YouTube).
