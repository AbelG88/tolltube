package com.peajecognitivo.app

import android.content.Context

/** Temas que el asistente le ofrece al padre. Cada uno trae uno o más módulos incluidos en la app. */
enum class Interes(val etiqueta: String, val archivos: List<String>) {
    ANIMALES("Animales", listOf("animales.json", "sonidos-animales.json")),
    DINOSAURIOS("Dinosaurios", listOf("dinosaurios.json")),
    VEHICULOS("Vehículos", listOf("transportes.json")),
    COLORES_FORMAS("Colores y formas", listOf("colores.json", "formas.json")),
    LETRAS_NUMEROS("Letras y números", listOf("letras.json", "numeros-en-letras.json")),
    CUERPO_EMOCIONES("Cuerpo y emociones", listOf("cuerpo.json", "emociones.json")),
    NATURALEZA("Naturaleza", listOf("clima.json", "frutas.json")),
    RUTINA("Rutina y tiempo", listOf("dias-meses.json", "secuencias.json", "todos-los-dias.json")),
    OBJETOS("Cosas de todos los días", listOf("objetos.json"))
}

/**
 * Los módulos del repo (carpeta /modulos) viajan dentro del APK como assets.
 * Se instalan sin internet: la app no tiene (ni necesita) permiso de red.
 */
object CatalogoModulos {

    /** Instala los módulos de los temas elegidos. Devuelve cuántos quedaron instalados. */
    fun instalar(context: Context, intereses: Set<Interes>): Int {
        var instalados = 0
        intereses.flatMap { it.archivos }.distinct().forEach { archivo ->
            try {
                val texto = context.assets.open(archivo).bufferedReader(Charsets.UTF_8).use { it.readText() }
                ModulosStore.importar(context, texto)
                instalados++
            } catch (e: Exception) {
                PeajeLog.d("CATALOGO no se pudo instalar '$archivo': ${e.javaClass.simpleName}")
            }
        }
        return instalados
    }
}
