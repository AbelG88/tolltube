package com.peajecognitivo.app

import android.content.Context
import kotlin.random.Random

/**
 * Genera un ejercicio al azar entre los tipos que el padre dejó tildados en el menú.
 * Usa solo emojis conocidos de versiones viejas de Unicode para que se vean en Android 8+.
 */
object ExerciseGenerator {

    private var ultimoResumen: String? = null

    private val frutas = listOf("🍎", "🍌", "🍓", "🍇", "🍊", "🍒")

    private val animalesEmoji = listOf("🐶", "🐱", "🐭", "🐰", "🐻", "🐼", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵")

    fun siguiente(context: Context): Exercise {
        val tipos = SettingsStore.getTipos(context).toList()
        val nivel = SettingsStore.getNivel(context)
        val hayModulos = ModulosStore.hayActivos(context)

        fun uno(): Exercise {
            // Los módulos activos cuentan como una opción más junto a los tipos de fábrica tildados
            val deModulo = hayModulos && (tipos.isEmpty() || Random.nextInt(tipos.size + 1) == tipos.size)
            if (deModulo) ModulosStore.alAzar(context)?.let { return it }
            return generar(if (tipos.isEmpty()) ExerciseType.SUMA else tipos.random(), nivel)
        }

        var ejercicio = uno()
        var intentos = 0
        // Evita repetir exactamente el mismo ejercicio dos veces seguidas
        while (ejercicio.resumen == ultimoResumen && intentos < 10) {
            ejercicio = uno()
            intentos++
        }
        ultimoResumen = ejercicio.resumen
        return ejercicio
    }

    private fun generar(tipo: ExerciseType, nivel: Int): Exercise = when (tipo) {
        ExerciseType.SUMA -> suma(nivel)
        ExerciseType.RESTA -> resta(nivel)
        ExerciseType.CONTAR -> contar(nivel)
        ExerciseType.MODULOS -> suma(nivel) // no se usa: los módulos salen de ModulosStore
    }

    private fun suma(nivel: Int): Exercise {
        val tope = when (nivel) { 1 -> 3; 2 -> 5; else -> 6 }
        val a = Random.nextInt(1, tope + 1)
        val b = Random.nextInt(1, tope + 1)
        val fruta = frutas.random()
        val visual = "${fruta.repeat(a)}\n➕\n${fruta.repeat(b)}"
        return Exercise(
            tipo = ExerciseType.SUMA,
            visual = visual,
            pregunta = "$a + $b = ?",
            opciones = null,
            correcta = (a + b).toString(),
            resumen = "suma $a+$b",
            hablado = "$a más $b. ¿Cuánto es?"
        )
    }

    private fun resta(nivel: Int): Exercise {
        val a = when (nivel) { 1 -> Random.nextInt(2, 5); 2 -> Random.nextInt(2, 7); else -> Random.nextInt(4, 7) }
        val b = Random.nextInt(1, a)   // 1..a-1, el resultado nunca es 0 ni negativo
        val fruta = frutas.random()
        val visual = "${fruta.repeat(a)}\n➖\n${fruta.repeat(b)}"
        return Exercise(
            tipo = ExerciseType.RESTA,
            visual = visual,
            pregunta = "$a − $b = ?",
            opciones = null,
            correcta = (a - b).toString(),
            resumen = "resta $a-$b",
            hablado = "$a menos $b. ¿Cuánto es?"
        )
    }

    private fun contar(nivel: Int): Exercise {
        val tope = when (nivel) { 1 -> 3; 2 -> 5; else -> 8 }
        val n = Random.nextInt(1, tope + 1)
        val dibujo = (frutas + animalesEmoji).random()
        return Exercise(
            ExerciseType.CONTAR, dibujo.repeat(n), "¿Cuántos hay?", null, n.toString(), "contar $n"
        )
    }
}
