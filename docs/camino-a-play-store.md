# Camino a Google Play

Investigado el 2026-10-16 con las páginas oficiales de Google. Lo que no se pudo confirmar está marcado.

## Cuenta de desarrollador
- Pago único de US$25 (tarjeta, no prepaga), mayor de 18, verificación de identidad con documento y tarjeta.
- Cuenta Personal u Organización. Personal creada después del 13/11/2023: prueba cerrada con **12 testers inscriptos 14 días seguidos** antes de pedir producción. No confirmado si aplica a Organización.
- Argentina soporta registro de desarrollador y de cuenta de cobro (moneda USD). No confirmado si los compradores argentinos pueden pagar apps.

## Riesgo principal: servicio de accesibilidad
La política de Google reserva el rol de "herramienta de accesibilidad" a servicios para personas con discapacidad; las apps de monitoreo no lo son. Pueden usar la API, pero con declaración, divulgación destacada dentro de la app y video demostrativo. Las apps de control parental no se mencionan expresamente (incertidumbre). Por eso se migra a `UsageStatsManager` (release 9).

No verificado: las reglas de Play para `PACKAGE_USAGE_STATS` y para `SYSTEM_ALERT_WINDOW`, y la declaración del servicio en primer plano de tipo `specialUse` (Play Console pide describir el uso).

## Política de Familias
Con público objetivo adultos (los padres) no aplican las reglas de Familias, pero hay que marcar los grupos de edad con exactitud. Sin anuncios, cuentas ni analíticas.

## Verificación de desarrolladores de Android
Desde el 30/9/2026 solo se instalan apps de desarrolladores registrados en Brasil, Indonesia, Singapur y Tailandia; en 2027 se extiende al resto. Existen cuentas de "distribución limitada" (hasta 20 dispositivos, sin ID ni costo).

## Pendientes técnicos antes de publicar
- [x] Detectar YouTube sin accesibilidad (release 9, modo "Uso de apps").
- [ ] Probar el modo nuevo en uso real y, si anda bien, dejarlo por defecto.
- [ ] Sacar el servicio de accesibilidad del manifest y del código.
- [ ] Clave de firma propia y paquete AAB (hoy se firma con la clave de debug).
- [ ] Quitar o limitar el registro de diagnóstico (anota cada app que se abre).
- [ ] Política de privacidad con URL pública, formulario de seguridad de datos y cuestionario de clasificación de contenido.
- [ ] Revisar el nivel de Android objetivo que exige Google (hoy 34; no verificado el mínimo vigente).
- [ ] Nombre de paquete `com.peajecognitivo.app`: queda fijo al publicar.

## Otros
- App Store de Apple: sería una app aparte; no investigado.
- Cuidar el lenguaje terapéutico (autismo / ABA) para no prometer resultados.
- Consulta informal a RR.HH. por compatibilidad con el empleo público.

## Fuentes
- https://support.google.com/googleplay/android-developer/answer/6112435
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/9893335
- https://support.google.com/googleplay/android-developer/table/3539140
- https://www.helpnetsecurity.com/?p=375237
