package com.peajecognitivo.app

enum class ExerciseType(val clave: String, val etiqueta: String) {
    SUMA("suma", "➕ Sumas (con dibujos)"),
    RESTA("resta", "➖ Restas (con dibujos)"),
    CONTAR("contar", "🔢 Contar"),
    MODULOS("modulos", "📦 Módulos importados");

    companion object {
        fun desdeClave(clave: String): ExerciseType? = values().firstOrNull { it.clave == clave }
    }
}

/**
 * Un ejercicio listo para mostrar en el overlay.
 *
 * - visual: emojis grandes (los dibujos)
 * - pregunta: el texto que se lee debajo ("3 + 2 = ?" o "¿Qué animal es?")
 * - opciones: null si se responde con número (teclado); si no, las 3 opciones para tocar
 * - correcta: la respuesta correcta (número como texto, o el nombre de la opción)
 * - resumen: texto corto para el registro
 */
data class Exercise(
    val tipo: ExerciseType,
    val visual: String,
    val pregunta: String,
    val opciones: List<String>?,
    val correcta: String,
    val resumen: String,
    /** Lo que lee la voz (si está activada). Por defecto, la pregunta. */
    val hablado: String = pregunta
)
