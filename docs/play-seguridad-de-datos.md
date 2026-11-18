# Respuestas sugeridas — Seguridad de los datos (Play Console)

Referencia para completar el formulario. Verificar en pantalla, porque Google cambia las preguntas.

## Respuestas
- ¿La app recopila o comparte datos de usuario? → **No.** Todo se procesa y queda en el teléfono: sin permiso de Internet, sin SDK de terceros, sin copias de seguridad en la nube (`allowBackup=false`).
- Datos que se tratan solo en el dispositivo (nombre o apodo y edad del niño, progreso, PIN, apps elegidas): Google no los considera "recopilados" si no salen del dispositivo. Si la consola insiste, declararlos como **no compartidos, procesados localmente, opcionales**.
- Cifrado en tránsito → no aplica (no hay transmisión).
- Pedido de eliminación de datos → no aplica (se borra al desinstalar o quitando al niño desde la app).
- Cuenta de usuario → no hay.
- Política de privacidad → URL pública: ver abajo.

## URL de la política de privacidad
Activar GitHub Pages: repositorio → Settings → Pages → Source: **Deploy from a branch**, branch `main`, carpeta `/docs`. La URL queda:
`https://abelg88.github.io/tranquera/politica-de-privacidad`
(tarda 1 a 2 minutos en aparecer). Pegar esa URL en Play Console y en la ficha.

## Otras declaraciones
- **Público objetivo:** adultos (padres, madres, tutores). No marcar a menores de 13 como público. Es una herramienta para adultos que se usa con sus hijos.
- **Anuncios:** no tiene.
- **Permisos sensibles:** ver `play-declaraciones.md`.
- **Apoyo voluntario (enlaces a Mercado Pago, Cafecito, PayPal, Buy Me a Coffee):** se abren en el navegador, no son compras dentro de la app ni contenido digital. Es la parte con más riesgo de observación de Google (Políticas de pagos); ver `play-ficha.md`.
