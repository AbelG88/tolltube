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

    private val animales = listOf(
        "🐶" to "Perro", "🐱" to "Gato", "🐭" to "Ratón", "🐰" to "Conejo",
        "🐻" to "Oso", "🐼" to "Panda", "🐯" to "Tigre", "🦁" to "León",
        "🐮" to "Vaca", "🐷" to "Cerdo", "🐸" to "Rana", "🐵" to "Mono",
        "🐔" to "Gallina", "🐧" to "Pingüino", "🐴" to "Caballo", "🐘" to "Elefante",
        "🐢" to "Tortuga", "🐍" to "Serpiente", "🐙" to "Pulpo", "🐠" to "Pez",
        "🦋" to "Mariposa", "🐝" to "Abeja", "🐌" to "Caracol", "🦆" to "Pato"
    )

    private val objetos = listOf(
        "⚽" to "Pelota", "🚗" to "Auto", "🚌" to "Colectivo", "✈️" to "Avión",
        "🚲" to "Bicicleta", "🏠" to "Casa", "📚" to "Libros", "⏰" to "Reloj",
        "🔑" to "Llave", "🎈" to "Globo", "🍞" to "Pan", "☂️" to "Paraguas",
        "👟" to "Zapatilla", "🎸" to "Guitarra", "📱" to "Celular", "💡" to "Lamparita",
        "⭐" to "Estrella", "🌙" to "Luna", "☀️" to "Sol", "🌳" to "Árbol",
        "🌸" to "Flor", "🍕" to "Pizza", "🍦" to "Helado", "🎂" to "Torta",
        "🔔" to "Campana", "✏️" to "Lápiz", "👓" to "Anteojos"
    )

    fun siguiente(context: Context): Exercise {
        val tipos = SettingsStore.getTipos(context).toList()
        var ejercicio = generar(tipos.random())
        var intentos = 0
        // Evita repetir exactamente el mismo ejercicio dos veces seguidas
        while (ejercicio.resumen == ultimoResumen && intentos < 10) {
            ejercicio = generar(tipos.random())
            intentos++
        }
        ultimoResumen = ejercicio.resumen
        return ejercicio
    }

    private fun generar(tipo: ExerciseType): Exercise = when (tipo) {
        ExerciseType.SUMA -> suma()
        ExerciseType.RESTA -> resta()
        ExerciseType.ANIMALES -> eleccion(tipo, animales, "¿Qué animal es?")
        ExerciseType.OBJETOS -> eleccion(tipo, objetos, "¿Qué es esto?")
    }

    private fun suma(): Exercise {
        val a = Random.nextInt(1, 6)   // 1..5
        val b = Random.nextInt(1, 6)   // 1..5
        val fruta = frutas.random()
        val visual = "${fruta.repeat(a)}\n➕\n${fruta.repeat(b)}"
        return Exercise(
            tipo = ExerciseType.SUMA,
            visual = visual,
            pregunta = "$a + $b = ?",
            opciones = null,
            correcta = (a + b).toString(),
            resumen = "suma $a+$b"
        )
    }

    private fun resta(): Exercise {
        val a = Random.nextInt(2, 7)   // 2..6 (entran en una línea)
        val b = Random.nextInt(1, a)   // 1..a-1, el resultado nunca es 0 ni negativo
        val fruta = frutas.random()
        val visual = "${fruta.repeat(a)}\n➖\n${fruta.repeat(b)}"
        return Exercise(
            tipo = ExerciseType.RESTA,
            visual = visual,
            pregunta = "$a − $b = ?",
            opciones = null,
            correcta = (a - b).toString(),
            resumen = "resta $a-$b"
        )
    }

    private fun eleccion(
        tipo: ExerciseType,
        lista: List<Pair<String, String>>,
        pregunta: String
    ): Exercise {
        val correcta = lista.random()
        val distractores = lista.filter { it.second != correcta.second }.shuffled().take(2)
        val opciones = (distractores.map { it.second } + correcta.second).shuffled()
        return Exercise(
            tipo = tipo,
            visual = correcta.first,
            pregunta = pregunta,
            opciones = opciones,
            correcta = correcta.second,
            resumen = "${tipo.clave} ${correcta.second}"
        )
    }
}
