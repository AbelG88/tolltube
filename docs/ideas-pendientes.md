# Ideas y pendientes

Lista viva. Lo hecho se tacha en [cambios.md](cambios.md).

## Plan de releases

| Release | Contenido | Estado |
|---|---|---|
| 4 (1.4) | Emociones (caras), voz (leer el ejercicio), ayuda tras errores, PIN de padres, pausa del video | hecho |
| 5 (1.5) | Estrellas / contador diario, orientación en el registro | hecho |
| 6 (1.6) | Más tipos: letras, colores, formas, contar, secuencias; niveles de dificultad; historial de aciertos por tipo | hecho |

## Idea en diseño: módulos de ejercicios importables

Pedido de Abel (2026-10-20): una pantalla de carga de módulos de ejercicios, temáticos o por edad, que se puedan importar y sacar.
Preguntas abiertas antes de construirlo:
- Formato del módulo (propuesta: un archivo JSON con nombre, edad sugerida y lista de ejercicios).
- Cómo se importa (desde un archivo del celu, o un enlace).
- Qué tipos de ejercicio puede traer un módulo (elegir opción, respuesta numérica, secuencia).
- Si los ejercicios actuales pasan a ser módulos "de fábrica" que también se pueden apagar.
- Derechos de las imágenes de módulos de terceros (para una futura versión comercial).

## Ideas

- **Emociones:** identificar caras felices, tristes, enojadas, asustadas (idea de Abel). Va en el release 4 con emojis.
- **Voz:** que el celu lea la pregunta en voz alta. Apagada por defecto, se activa en el menú.
- **Ayuda tras errores:** resaltar la opción correcta tras el primer error; en sumas y restas, mostrar la respuesta como pista tras dos errores. Evita la frustración.
- **PIN de padres:** para que el niño no pueda cambiar los ajustes ni ver el registro.
- **Aviso previo:** descartado por Abel.
- **Estrellas / contador diario:** refuerzo positivo, sin castigo.
- **Más tipos:** letras y sílabas, colores, formas, contar objetos, completar secuencias.
- **Niveles de dificultad** por tipo, e historial de aciertos para los padres.
- **Pictogramas ARASAAC:** descartados por ahora (licencia CC BY-NC-SA, la cláusula NC choca con una posible comercialización). Se usan emojis.

## Pendientes abiertos

- Confirmar en el celu la 1.3 (arreglo de H1): sumas con teclado repetidas, YouTube abierto antes de activar accesibilidad, salir a WhatsApp y volver, horizontal.
- Aclarar qué hay que corregir en el documento original del proyecto.
- H3, H5 y H7 del [diagnóstico](diagnostico-intermitencia.md) no se observaron ni se arreglaron.
- Arquitectura a futuro: el servicio de accesibilidad sirve para uso personal, pero para publicar en Play Store habría que migrar a UsageStatsManager / DevicePolicyManager.
- Política Play Familias: público declarado adultos; sin anuncios, cuentas ni analíticas.

## Forma de trabajo

Cada release se compila en GitHub Actions y se publica en https://github.com/AbelG88/tolltube/releases/latest. Los releases se verifican solo por compilación; la prueba en el celu es de Abel.
