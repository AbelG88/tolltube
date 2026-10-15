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

    private val emociones = listOf(
        "😊" to "Feliz", "😢" to "Triste", "😠" to "Enojado",
        "😨" to "Asustado", "😮" to "Sorprendido", "😴" to "Con sueño"
    )

    private val colores = listOf(
        "❤️" to "Rojo", "💛" to "Amarillo", "💚" to "Verde",
        "💙" to "Azul", "💜" to "Violeta", "🖤" to "Negro"
    )

    private val formas = listOf(
        "⚫" to "Círculo", "⬛" to "Cuadrado", "🔺" to "Triángulo",
        "⭐" to "Estrella", "🔷" to "Rombo", "❤️" to "Corazón"
    )

    private val letrasFaciles = listOf("A", "E", "I", "O", "U", "M", "P", "S", "L", "T", "C", "B")
    private val abecedario = ('A'..'Z').map { it.toString() }

    fun siguiente(context: Context): Exercise {
        val tipos = SettingsStore.getTipos(context).toList()
        val nivel = SettingsStore.getNivel(context)
        var ejercicio = generar(tipos.random(), nivel)
        var intentos = 0
        // Evita repetir exactamente el mismo ejercicio dos veces seguidas
        while (ejercicio.resumen == ultimoResumen && intentos < 10) {
            ejercicio = generar(tipos.random(), nivel)
            intentos++
        }
        ultimoResumen = ejercicio.resumen
        return ejercicio
    }

    private fun generar(tipo: ExerciseType, nivel: Int): Exercise = when (tipo) {
        ExerciseType.SUMA -> suma(nivel)
        ExerciseType.RESTA -> resta(nivel)
        ExerciseType.LETRAS -> letras(nivel)
        ExerciseType.COLORES -> eleccion(tipo, colores, "¿De qué color es?")
        ExerciseType.FORMAS -> eleccion(tipo, formas, "¿Qué forma es?")
        ExerciseType.CONTAR -> contar(nivel)
        ExerciseType.SECUENCIAS -> secuencia(nivel)
        ExerciseType.ANIMALES -> eleccion(tipo, animales, "¿Qué animal es?")
        ExerciseType.OBJETOS -> eleccion(tipo, objetos, "¿Qué es esto?")
        ExerciseType.EMOCIONES -> eleccion(tipo, emociones, "¿Cómo se siente?")
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

    private fun letras(nivel: Int): Exercise {
        if (nivel == 1) {
            // Reconocer la letra
            val correcta = letrasFaciles.random()
            val opciones = (letrasFaciles.filter { it != correcta }.shuffled().take(2) + correcta).shuffled()
            return Exercise(ExerciseType.LETRAS, correcta, "¿Qué letra es?", opciones, correcta, "letras $correcta")
        }
        // Con qué letra empieza el animal
        val animal = animales.random()
        val inicial = animal.second.first().toString()
        val opciones = (abecedario.filter { it != inicial }.shuffled().take(2) + inicial).shuffled()
        return Exercise(
            ExerciseType.LETRAS, animal.first, "¿Con qué letra empieza?", opciones, inicial,
            "letras inicial de ${animal.second}",
            hablado = "¿Con qué letra empieza el nombre de este animal?"
        )
    }

    private fun contar(nivel: Int): Exercise {
        val tope = when (nivel) { 1 -> 3; 2 -> 5; else -> 8 }
        val n = Random.nextInt(1, tope + 1)
        val dibujo = (frutas + animales.map { it.first }).random()
        return Exercise(
            ExerciseType.CONTAR, dibujo.repeat(n), "¿Cuántos hay?", null, n.toString(), "contar $n"
        )
    }

    private fun secuencia(nivel: Int): Exercise {
        val paso = when (nivel) {
            1 -> 1
            2 -> listOf(1, 2).random()
            else -> listOf(2, 3, 5, 10).random()
        }
        val largo = if (nivel == 1) 3 else 4
        val inicio = if (nivel == 3) Random.nextInt(0, 11) else Random.nextInt(1, 6)
        val numeros = (0 until largo).map { inicio + it * paso }
        val siguiente = inicio + largo * paso
        val texto = numeros.joinToString(" · ") + " · ?"
        return Exercise(
            ExerciseType.SECUENCIAS, "🔢", texto, null, siguiente.toString(),
            "secuencia ${numeros.joinToString(",")}",
            hablado = numeros.joinToString(", ") + ". ¿Qué número sigue?"
        )
    }
}
