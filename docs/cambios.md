# Historial de cambios

Cada push a `main` compila y publica un release en
https://github.com/AbelG88/tolltube/releases

## Pendientes pedidos (en orden)

- [x] Ícono relacionado con YouTube (release 1)
- [x] Tiempo configurable en minutos y segundos (release 1)
- [x] Sumas y restas con imágenes (emojis) acompañando los números (release 2)
- [x] Más tipos de ejercicio (animales, objetos) y elegirlos desde el menú (release 2)

## Release 20 — Todo con el nombre Tranquera y PIN estilo Nappy

- **Nombres internos al día:** el paquete y el `applicationId` pasan de `com.peajecognitivo.app` a `com.tranquera.app`; las clases `Peaje*` pasan a `Tranquera*`, y también los estilos, el tag del registro (`adb logcat -s Tranquera:D`), los archivos que publica el workflow (`tranquera.apk`, `tranquera.aab`) y la documentación. El repositorio de GitHub sigue llamándose `tolltube`.
- **Ojo al actualizar:** con otro `applicationId`, Android lo instala como una app **nueva** (no pisa a la anterior). Hay que desinstalar "Peaje Cognitivo" y se pierden sus datos (PIN, niños, progreso); se vuelve a hacer el asistente.
- **PIN estilo Nappy:** pantalla completa celeste con tarjeta blanca de esquinas muy redondeadas y candado blanco sobre cuadrado azul (el look de la pantalla de bloqueo de Nappy), con teclado numérico redondo, puntitos que se llenan, sacudida roja si está mal y confirmación automática. Ahora el PIN es de **4 números**. Se usa para entrar a los ajustes, cambiarlo, apagar desde la notificación y elegirlo en el asistente (en `PinPad.kt`).

## Release 19 — Nuevo nombre e ícono: Tranquera

- La app pasa a llamarse **Tranquera** (nombre visible, notificación, ejercicio, donaciones). El paquete interno `com.peajecognitivo.app` no cambia.
- Ícono nuevo: una tranquera con una estrella, sobre azul.

## Release 18 — Rediseño simple (v3.6.0)

- Asistente de 4 pasos: nombre y edad (el nivel sale de la edad), qué le gusta, qué apps cuidamos, permisos y PIN.
- Qué apps cuidamos, 3 modos: las de siempre (YouTube, YouTube Kids, TikTok, Instagram, Netflix), las que elijo de la lista de apps instaladas (juegos incluidos), o todo menos lo que permito. La lista usa `<queries>` (sin `QUERY_ALL_PACKAGES`).
- Inicio con un solo aviso de estado (verde "Todo funcionando" o ámbar con "Arreglar ahora"), tarjeta del niño, "Qué cuidamos" y "Probar ahora".
- Barra inferior Inicio / Progreso / Ajustes. Ajustes con modos Suave (20 min), Normal (10 min) e Intenso (2 ejercicios cada 5 min). Lo técnico quedó en "Avanzado".

## Release 17 — Estilo Nappy y apoyo voluntario (donaciones)

- **Estilo tomado de la app Nappy:** tipografía Quicksand (incluida en la app, licencia OFL en `licencias/`), azul `#3B82F6` sobre fondo celeste, tarjeta principal con degradé azul–índigo con el niño activo, dos fichas con "Aciertos hoy" y cada cuánto sale el ejercicio. El logo y el ícono pasaron al mismo azul.
- **Apoyo voluntario (mismo sistema que Nappy)**, en `Donaciones.kt`:
  - Enlaces: Mercado Pago y Cafecito (desde Argentina), PayPal y Buy Me a Coffee (desde el exterior).
  - Cuándo aparece el pedido: cada 100 aciertos resueltos (entre todos los niños) desde el último pedido, como un festejo: "¡Felicitaciones! Ya [nombre] hizo 100 ejercicios bien… ¿querés colaborar?". Nunca dos pedidos con menos de 7 días de separación; la fecha de instalación cuenta como último pedido, así que los primeros 7 días no se pide nada. "Ya doné" deja tranquilo 90 días. Espera 2,5 s y a que el PIN ya esté puesto. (Nappy también pide por ingresos; acá se descartó.)
  - **Solo se muestra en el panel de padres**, nunca sobre las apps del niño.
  - También hay una sección fija "Apoyá Peaje Cognitivo" en Más ajustes.
  - El contador es local (aciertos totales entre todos los niños). En instalaciones anteriores el total arranca con el progreso ya guardado.
- Versión 3.5.0-MVP (versionCode 26).

## Release 16 — Interfaz más amigable y permisos guiados

- **Estilo más cálido:** fondo lila suave, tarjetas llenas y muy redondeadas sin bordes, con tintes pastel (menta, lila, manteca), botones en pastilla y fichas (chips) que se pintan al elegirlas. Los estilos están en el tema (`Widget.Peaje.Button`, `Widget.Peaje.Card`, `Widget.Peaje.Chip`), así toda la app los toma.
- **Asistente:** cada paso tiene su emoji en un círculo de color y textos más cercanos.
- **Permisos guiados:** dos tarjetas (mostrar sobre otras apps, datos de uso) con botón "Activar" que pasa a "✓ Listo", y una tarjeta de ayuda con los 4 pasos para "configuración restringida" y botón a los ajustes de la app.
- Versión 3.4.0-MVP (versionCode 25).

## Release 15 — Logo nuevo y aviso de "configuración restringida"

- **Logo nuevo** (original): una barrera de peaje levantada con una chispa, en el azul de la app. Reemplaza el ícono anterior (ícono adaptable, `ic_launcher_*`) y se usa también en la bienvenida del asistente y en el encabezado de la pantalla principal (`ic_logo`).
- **Aviso de permisos bloqueados:** Android 13+ muestra "A la app se le negó el acceso" en las apps instaladas desde un archivo. El asistente (paso de permisos) y la sección Permisos explican dónde habilitarlo (Ajustes → Aplicaciones → Peaje Cognitivo → ⋮ → Permitir configuración restringida). El asistente suma un botón "Abrir ajustes de la app".
- Versión 3.3.1-MVP (versionCode 24).

## Release 14 — Varios niños, un panel por niño

- **Niños** (`Ninos.kt`): se pueden cargar hasta 6. Cada uno tiene sus propios ajustes (nivel, intervalo, ejercicios por vez, apps vigiladas, tipos, módulos activos) y su progreso.
- **El padre elige quién usa el celular** con las fichas de "Quién usa el celular" en la pantalla principal; el timer y las apps vigiladas siguen al niño activo. "+ Agregar" abre el asistente en modo niño nuevo (sin permisos ni PIN). "Quitar este niño" borra sus ajustes y progreso, con confirmación.
- **Sin migración:** el primer niño conserva las claves de siempre, así lo ya configurado y el progreso previo siguen intactos. El PIN y el interruptor general son únicos para toda la app.
- Los módulos incluidos en el APK se activan por niño según sus intereses.
- Versión 3.3.0-MVP (versionCode 23).

## Release 13 — Asistente de primera vez y pantalla principal simple

- **Asistente de configuración** (`AsistenteActivity`): una pregunta por pantalla — nombre del niño (opcional, solo local), intereses, tipos de cuenta, nivel, cada cuánto aparece el ejercicio, cuántos por vez, apps vigiladas, permisos y PIN de padres. Nada se guarda hasta el último paso. Se abre sola la primera vez y se puede repetir desde "Repetir asistente" (pide el PIN).
- **Módulos incluidos en el APK** (`CatalogoModulos`, Gradle `assets.srcDirs("../modulos")`): los intereses elegidos instalan sus módulos sin internet; la app sigue sin permiso INTERNET.
- **Pantalla principal más simple**: interruptor, resumen de la configuración, estado, progreso y "Probar ahora". Los ajustes detallados quedan detrás de "Más ajustes".
- El mensaje al terminar la serie usa el nombre del niño si está cargado. El nombre nunca se escribe en el registro.
- Instalaciones que ya estaban configuradas no ven el asistente.
- Versión 3.2.0-MVP (versionCode 22).

## Release 12: seguridad del PIN y del registro (versión 3.1.0-MVP)

- **El PIN ya no se guarda en texto plano.** Se guarda un hash PBKDF2-SHA256 (120.000 vueltas) con sal aleatoria. Si ya tenías un PIN, se convierte solo la primera vez que se usa.
- **Bloqueo por intentos.** Tras 5 PIN incorrectos seguidos se bloquea el ingreso 30 s, y se duplica con cada fallo (máximo 15 min). El conteo se guarda en disco: cerrar la app no lo borra. Vale para la pantalla de ajustes, el registro y el "Apagar" de la notificación.
- **PIN obligatorio.** Si no hay PIN, la app pide crear uno al abrirse y no se puede saltear. Ya no se puede quitar: solo cambiar.
- **Para cambiar el PIN hay que ingresar el actual** (y el nuevo se escribe dos veces).
- **El PIN se vuelve a pedir al salir de la app** (3 s después de que toda la app sale de pantalla o se apaga la pantalla), en vez de quedar abierto 60 s. Girar el celu no lo vuelve a pedir.
- **Registro de diagnóstico sin datos sensibles.** Ya no anota el nombre de la app en pantalla (solo "vigilada", "otra" o "ninguna") ni el ejercicio con su respuesta (solo el tipo). Al copiarlo se marca como contenido sensible (Android 13+).

## Release 11

- **Rediseño minimalista.** Tema Material 3 sobrio con un solo color de acento, tarjetas, botones redondeados y chips en lugar de tildes (tipos de ejercicio, dificultad, módulos importados y apps vigiladas). El overlay sobre la app usa vistas nativas redondeadas.
- **Un error ya no muestra la respuesta.** Se eliminó la pantalla de reflexión, la pista tras 2 errores y la voz que decía la respuesta. Ahora solo dice "Esa no es. Probá de nuevo" y el ejercicio queda trabado hasta acertar. En los ejercicios de tocar una opción, tras cada error los botones se traban unos segundos (cada vez más: 2, 4, 6… hasta 10 s) y las opciones cambian de lugar.
- **Apps vigiladas** (nueva sección con chips): YouTube, YouTube Kids, TikTok e Instagram. YouTube y YouTube Kids vienen activadas (como antes); TikTok e Instagram, apagadas. Tiene que quedar al menos una. Cubre las variantes Lite/Go y la versión regional de TikTok. El timer corre mientras haya una app vigilada en pantalla; con "contar solo mientras suena audio" solo cuenta si hay audio.
  - Instagram y TikTok no siempre se pausan al aparecer el ejercicio (la pausa depende de que la app ceda el audio), y en Instagram el tiempo corre también si se usa para mensajes.
- **Notificación fija útil.** Tocarla abre la app (que pide el PIN si hay uno). Tiene una acción "Apagar (PIN)" que apaga el peaje y detiene el servicio; sin PIN configurado no apaga nada (avisa que hay que poner uno). Tras 5 PIN incorrectos se bloquea unos segundos, cada vez más. Se vuelve a prender con el interruptor de la app.
- Apagar el interruptor "Peaje activado" ahora también detiene el servicio (desaparece la notificación) y al reiniciar el celu el monitoreo no arranca si está apagado.
- Internamente `Pantalla.YOUTUBE` pasa a `Pantalla.VIGILADA`.

## Release 10: versión 2.0-MVP

- **Modo "Uso de apps" por defecto.** Los pasos de la pantalla principal son ahora solo dos: permiso de dibujar sobre otras apps y acceso a datos de uso.
- **Accesibilidad pasa a "⚙️ Avanzado"** (se despliega con el botón): ahí se elige el modo de detección y se activa el servicio de accesibilidad, solo por si el modo normal no anda en algún celu. Si ya tenías guardado el modo accesibilidad, se respeta.
- **Contar solo mientras se reproduce audio** (interruptor, activado por defecto): el tiempo hasta el próximo ejercicio solo corre si YouTube está en pantalla Y hay audio sonando. Si no, la cuenta se queda quieta y retoma donde estaba. El panel de estado lo indica ("en pausa: no hay YouTube sonando") y el registro anota `TIMER cuenta en pausa / reanudada`. Apagado, funciona como antes.
- Al arrancar la app se pide el permiso de notificaciones si falta (la notificación fija del monitoreo lo necesita en Android 13+).

## Release 9.1: versión 1.9.1-MVP

- En modo "Uso de apps", el servicio de accesibilidad ya no anota los cambios de ventana en el registro (llenaban las últimas 300 líneas y tapaban las líneas `USO` y `MONITOR`). En modo "Accesibilidad" todo sigue igual.

## Release 9: versión 1.9-MVP

Primer paso del camino a Google Play (ver [camino-a-play-store.md](camino-a-play-store.md)): detectar YouTube **sin servicio de accesibilidad**.

- **Dos modos de detección**, a elegir en la pantalla principal ("🔍 Cómo detecta YouTube"):
  - **Accesibilidad (como hasta ahora)**: es el valor por defecto, para no romper lo que ya anda.
  - **Uso de apps (nuevo)**: usa `UsageStatsManager`. Pide el permiso especial "Acceso a datos de uso" (botón 3) y corre un servicio en primer plano con una notificación fija ("Peaje Cognitivo - Cuidando el tiempo de pantalla"). Cada 1,5 s mira qué app está en primer plano.
- **El resto no cambia:** el timer, el overlay, la pausa del video, las series y todo lo demás funcionan igual en los dos modos. Solo cambia cómo se sabe que YouTube está en pantalla.
- **Arranque con el celu:** en modo nuevo, el monitoreo se reactiva al prender el teléfono.
- **Panel de estado** y **registro** muestran el modo: líneas `USO app en primer plano: ...` y `MONITOR servicio iniciado/detenido`.
- Al cambiar de modo se detiene el timer y se vuelve a arrancar solo al detectar YouTube.

Qué probar con el modo nuevo: ver [camino-a-play-store.md](camino-a-play-store.md).

## Release 8: versión 1.8-MVP

- **Interruptor "Peaje activado / APAGADO"** arriba de todo en la pantalla principal. Apagado, el timer se detiene y el ejercicio no aparece nunca (el botón "Probar overlay" sigue andando). Está detrás del PIN, y el estado se ve en el panel.
- **Ejercicios seguidos:** nuevo campo junto al tiempo (1 a 20). Con "30 segundos" y "5", cada 30 s hay que resolver 5 ejercicios juntos y recién ahí se libera el video. El título del overlay dice "2 de 5". Los errores no reinician la cuenta: el ejercicio fallado se reemplaza por otro y sigue la serie. El tiempo hasta la próxima serie se cuenta desde que se resuelve la última.
- Un solo botón guarda tiempo y cantidad.

## Release 7: versión 1.7-MVP

Motivo: probando con el usuario final, elegía opciones incorrectas hasta que quedaba la correcta (por descarte).

- **Opción incorrecta → pantalla "Esa no es. Leé con atención".** Tapa todo, muestra el dibujo, la pregunta y la respuesta correcta, y la lee en voz alta si la voz está activada. Hay una cuenta regresiva y el botón "Entendí, probar de nuevo" recién se habilita al terminar. Nunca desbloquea la pantalla.
- **Ejercicio nuevo después.** Al tocar "Entendí" sale un ejercicio distinto (no se puede ir descartando opciones del mismo).
- **La espera crece con los errores seguidos:** 5, 8, 11 y 12 segundos (tope). Se reinicia al acertar.
- **Se sacó la ayuda de resaltar en verde la opción correcta** (ya no hace falta y permitía acertar sin pensar).
- Los errores siguen sumando al progreso por tipo. Las sumas y restas con teclado no cambian.

## Release 6.1: versión 1.6.1-MVP

- **El campo de respuesta y el botón OK pasaron arriba** del ejercicio (justo debajo del título), y todo el contenido está pegado al borde superior. En Android nuevo el teclado volvía a tapar el botón.
- La ventana del ejercicio ahora se achica cuando aparece el teclado, así que si hace falta se puede desplazar.

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

## Release 11 — 2.1-MVP: módulos de ejercicios importables
- Nueva pantalla "📦 Módulos de ejercicios" (protegida por PIN): importa archivos `.json` desde el teléfono, los lista con interruptor de activado y botón para quitar.
- Tipos de ejercicio en un módulo: `opciones` (3 botones) y `numero` (teclado). Solo texto/emojis; no se cargan imágenes ni código de afuera.
- Interruptor "Usar también los ejercicios de fábrica": apagado, salen solo los módulos activos (si no hay ninguno, se usan los de fábrica).
- Validación con mensaje claro si el archivo está mal (máx. 200 ejercicios, 300 KB).
- Botón "Copiar modelo" y ejemplo en `docs/modulos-ejemplo/dinosaurios.json`.

## Release 12 — 2.2-MVP: ejercicios de fábrica reducidos a 3
- De fábrica quedan solo **Sumas, Restas y Contar** (se generan con código).
- Animales, Objetos, Emociones, Colores, Formas, Letras y Secuencias pasaron a **módulos** `.json` en la carpeta `modulos/` del repo. Ya no vienen instalados: se importan desde "📦 Módulos de ejercicios".
- Cada release del APK también adjunta los `.json` de `modulos/` para bajarlos directo desde el teléfono.

## Release 13 — 2.3-MVP: módulos en la pantalla principal + 9 módulos nuevos
- Los módulos importados aparecen en la pantalla principal junto a los tipos de ejercicio, con fondo celeste, casilla de activado y botón "Quitar". Los de fábrica (Sumas, Restas, Contar) no tienen "Quitar".
- Al importar un módulo se vuelve a la pantalla principal. Se eliminó el interruptor "usar los de fábrica": ahora se destildan los de fábrica directamente (tiene que quedar al menos un tipo o módulo activo).
- Módulos nuevos en `modulos/`: Dinosaurios, Frutas, Transportes, Clima, Cuerpo humano, Sonidos de animales, Números de todos los días, Números en letras, Días y meses.

## Release 14 — 3.0-MVP: camino a Google Play
- **Se eliminó el servicio de accesibilidad** (código, manifiesto, configuración y la sección "Avanzado"). La detección es solo por acceso a datos de uso.
- **Registro de diagnóstico fácil de sacar:** se controla con `BuildConfig.LOG_DIAGNOSTICO` en `app/build.gradle.kts`: `true` en debug (APK de pruebas), `false` en release (lo que va a Play). En release no se escribe nada, se borra cualquier registro viejo y el botón "Ver registro" se oculta. Para quitarlo del todo antes de publicar alcanza con subir la versión release; si se quiere borrar el código, ver `PeajeLog.kt` y `LogActivity.kt`.
- **Bundle .aab firmado con clave de subida propia:** el workflow arma `peaje-cognitivo.aab` (artefacto del run) si están cargados los secrets `RELEASE_KEYSTORE_B64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Sin secrets se omite. El APK de pruebas sigue con la clave de debug (la versión de Play tendrá otra firma: no se instala encima de la de pruebas).
- Cada build también verifica que compila la variante release.

## Release 15 — 3.0.1-MVP: privacidad
- `android:allowBackup="false"`: el PIN y el progreso no se copian a la nube de Google (coherente con la política).
- Nuevos docs: `docs/politica-de-privacidad.md` y `docs/play-seguridad-de-datos.md` (respuestas sugeridas del formulario).
