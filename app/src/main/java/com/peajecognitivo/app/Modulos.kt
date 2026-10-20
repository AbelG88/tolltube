package com.peajecognitivo.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/** Un ejercicio dentro de un módulo importado. */
class ItemModulo(
    val tipo: String,              // "opciones" o "numero"
    val visual: String,
    val pregunta: String,
    val hablado: String?,
    val correcta: String,
    val incorrectas: List<String>  // solo para "opciones"
)

/** Un módulo: un paquete de ejercicios temático o por edad, importado desde un archivo JSON. */
class Modulo(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val edad: String,
    val items: List<ItemModulo>
)

/**
 * Módulos importados. Cada uno se guarda como archivo en la carpeta privada de la app
 * (filesDir/modulos) y tiene un interruptor de activado/desactivado.
 * Son solo texto: no ejecutan código ni cargan imágenes de afuera.
 */
object ModulosStore {
    const val FORMATO = 1
    const val MAX_BYTES = 300 * 1024
    private const val MAX_EJERCICIOS = 200
    private const val MAX_TEXTO = 200

    private var cache: List<Modulo>? = null

    private fun carpeta(context: Context): File =
        File(context.filesDir, "modulos").also { it.mkdirs() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("peaje_prefs", Context.MODE_PRIVATE)

    /** Lee y valida el texto de un módulo. Si algo está mal, lanza un error con el motivo. */
    fun parsear(texto: String): Modulo {
        val raiz = try {
            JSONObject(texto.trim().removePrefix("﻿"))
        } catch (e: Exception) {
            throw IllegalArgumentException("El archivo no es un JSON válido")
        }
        if (raiz.optInt("formato", 0) != FORMATO) {
            throw IllegalArgumentException("Falta \"formato\": $FORMATO")
        }
        val nombre = texto(raiz, "nombre", obligatorio = true)
        val lista: JSONArray = raiz.optJSONArray("ejercicios")
            ?: throw IllegalArgumentException("Falta la lista \"ejercicios\"")
        if (lista.length() < 1) throw IllegalArgumentException("El módulo no tiene ejercicios")
        if (lista.length() > MAX_EJERCICIOS) {
            throw IllegalArgumentException("Máximo $MAX_EJERCICIOS ejercicios por módulo")
        }

        val items = ArrayList<ItemModulo>()
        for (i in 0 until lista.length()) {
            val n = i + 1
            val o = lista.optJSONObject(i)
                ?: throw IllegalArgumentException("Ejercicio $n: no es un objeto")
            val tipo = o.optString("tipo")
            val pregunta = texto(o, "pregunta", obligatorio = true, donde = "Ejercicio $n")
            val visual = texto(o, "visual", obligatorio = false, donde = "Ejercicio $n")
            val hablado = texto(o, "hablado", obligatorio = false, donde = "Ejercicio $n").ifEmpty { null }
            val correcta = texto(o, "correcta", obligatorio = true, donde = "Ejercicio $n")
            when (tipo) {
                "opciones" -> {
                    val arr = o.optJSONArray("incorrectas")
                        ?: throw IllegalArgumentException("Ejercicio $n: faltan las \"incorrectas\"")
                    val incorrectas = ArrayList<String>()
                    for (j in 0 until arr.length()) {
                        val t = arr.optString(j).trim()
                        if (t.isEmpty() || t.length > MAX_TEXTO) {
                            throw IllegalArgumentException("Ejercicio $n: una incorrecta está vacía o es muy larga")
                        }
                        if (t != correcta && t !in incorrectas) incorrectas.add(t)
                    }
                    if (incorrectas.size < 2) {
                        throw IllegalArgumentException("Ejercicio $n: hacen falta al menos 2 incorrectas distintas de la correcta")
                    }
                    items.add(ItemModulo(tipo, visual, pregunta, hablado, correcta, incorrectas))
                }
                "numero" -> {
                    if (!Regex("^\\d{1,6}$").matches(correcta)) {
                        throw IllegalArgumentException("Ejercicio $n: la correcta tiene que ser un número entero (solo dígitos)")
                    }
                    items.add(ItemModulo(tipo, visual, pregunta, hablado, correcta, emptyList()))
                }
                else -> throw IllegalArgumentException("Ejercicio $n: \"tipo\" tiene que ser \"opciones\" o \"numero\"")
            }
        }
        return Modulo(
            id = idDe(nombre),
            nombre = nombre,
            descripcion = texto(raiz, "descripcion", obligatorio = false),
            edad = texto(raiz, "edad", obligatorio = false),
            items = items
        )
    }

    private fun texto(o: JSONObject, clave: String, obligatorio: Boolean, donde: String = "El módulo"): String {
        val t = o.optString(clave, "").trim()
        if (obligatorio && t.isEmpty()) throw IllegalArgumentException("$donde: falta \"$clave\"")
        if (t.length > MAX_TEXTO) throw IllegalArgumentException("$donde: \"$clave\" es muy largo (máximo $MAX_TEXTO)")
        return t
    }

    private fun idDe(nombre: String): String {
        val base = nombre.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)
        return base.ifEmpty { "modulo" }
    }

    /** Importa un módulo. Si ya había uno con el mismo nombre, se reemplaza. */
    fun importar(context: Context, texto: String): Modulo {
        val m = parsear(texto)
        File(carpeta(context), "${m.id}.json").writeText(texto, Charsets.UTF_8)
        cache = null
        if (!prefs(context).contains("modulo_on_${m.id}")) setActivo(context, m.id, true)
        PeajeLog.d("MODULOS importado '${m.nombre}' (${m.items.size} ejercicios)")
        return m
    }

    fun listar(context: Context): List<Modulo> {
        cache?.let { return it }
        val lista = (carpeta(context).listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
            .sortedBy { it.name }
            .mapNotNull {
                try {
                    parsear(it.readText(Charsets.UTF_8))
                } catch (e: Exception) {
                    PeajeLog.d("MODULOS archivo ${it.name} inválido: ${e.message}")
                    null
                }
            }
        cache = lista
        return lista
    }

    fun activo(context: Context, id: String): Boolean = prefs(context).getBoolean("modulo_on_$id", true)

    fun setActivo(context: Context, id: String, v: Boolean) {
        prefs(context).edit().putBoolean("modulo_on_$id", v).apply()
    }

    fun borrar(context: Context, id: String) {
        File(carpeta(context), "$id.json").delete()
        prefs(context).edit().remove("modulo_on_$id").apply()
        cache = null
        PeajeLog.d("MODULOS quitado '$id'")
    }

    fun hayActivos(context: Context): Boolean =
        listar(context).any { activo(context, it.id) }

    /** Un ejercicio al azar de los módulos activados (null si no hay). */
    fun alAzar(context: Context): Exercise? {
        val modulo = listar(context).filter { activo(context, it.id) }.randomOrNull() ?: return null
        val it = modulo.items.random()
        val opciones = if (it.tipo == "opciones") {
            (it.incorrectas.shuffled().take(2) + it.correcta).shuffled()
        } else null
        return Exercise(
            tipo = ExerciseType.MODULOS,
            visual = it.visual,
            pregunta = it.pregunta,
            opciones = opciones,
            correcta = it.correcta,
            resumen = "modulo ${modulo.nombre}: ${it.pregunta} ${it.correcta}",
            hablado = it.hablado ?: it.pregunta
        )
    }

    val EJEMPLO = """{
  "formato": 1,
  "nombre": "Los dinosaurios",
  "descripcion": "Ejercicios sobre dinosaurios",
  "edad": "4 a 6 años",
  "ejercicios": [
    {
      "tipo": "opciones",
      "visual": "🦖",
      "pregunta": "¿Cómo se llama este dinosaurio?",
      "correcta": "T-Rex",
      "incorrectas": ["Perro", "Gato"]
    },
    {
      "tipo": "numero",
      "visual": "🦕🦕🦕",
      "pregunta": "¿Cuántos dinosaurios hay?",
      "correcta": "3"
    }
  ]
}"""
}
