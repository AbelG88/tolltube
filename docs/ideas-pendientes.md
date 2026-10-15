# Ideas y pendientes

Lista viva. Lo hecho se tacha en [cambios.md](cambios.md).

## Plan de releases

| Release | Contenido | Estado |
|---|---|---|
| 4 (1.4) | Emociones (caras), voz (leer el ejercicio), ayuda tras errores, PIN de padres, pausa del video | hecho |
| 5 (1.5) | Estrellas / contador diario, orientación en el registro | hecho; falta el aviso suave previo |
| 6 (1.6) | Más tipos: letras, colores y formas, contar, secuencias; niveles de dificultad; historial de aciertos por tipo | idea |

## Ideas

- **Emociones:** identificar caras felices, tristes, enojadas, asustadas (idea de Abel). Va en el release 4 con emojis.
- **Voz:** que el celu lea la pregunta en voz alta. Apagada por defecto, se activa en el menú.
- **Ayuda tras errores:** resaltar la opción correcta tras el primer error; en sumas y restas, mostrar la respuesta como pista tras dos errores. Evita la frustración.
- **PIN de padres:** para que el niño no pueda cambiar los ajustes ni ver el registro.
- **Aviso previo:** un cartelito "En 10 segundos viene un ejercicio" para anticipar (útil con niños que se frustran con cortes bruscos).
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
