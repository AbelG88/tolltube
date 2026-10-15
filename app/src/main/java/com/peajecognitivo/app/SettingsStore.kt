package com.peajecognitivo.app

import android.content.Context

/**
 * Ajustes guardados en el celular (SharedPreferences):
 * - tiempo entre ejercicios (en segundos)
 * - tipos de ejercicio habilitados
 */
object SettingsStore {
    private const val PREFS = "peaje_prefs"
    private const val KEY_INTERVAL_SECONDS = "interval_seconds"
    private const val KEY_TIPOS = "tipos_ejercicio"

    const val DEFAULT_SECONDS = 60          // 1 minuto (igual que antes)
    const val MIN_SECONDS = 10              // por debajo de esto es incómodo
    const val MAX_SECONDS = 3 * 60 * 60     // 3 horas

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getIntervalSeconds(context: Context): Int =
        prefs(context).getInt(KEY_INTERVAL_SECONDS, DEFAULT_SECONDS)
            .coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun getIntervalMs(context: Context): Long = getIntervalSeconds(context) * 1000L

    fun setIntervalSeconds(context: Context, seconds: Int) {
        prefs(context).edit()
            .putInt(KEY_INTERVAL_SECONDS, seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
            .apply()
    }

    /** Tipos de ejercicio habilitados. Si no hay nada guardado, están todos. Nunca devuelve vacío. */
    fun getTipos(context: Context): Set<ExerciseType> {
        val guardados = prefs(context).getStringSet(KEY_TIPOS, null)
        val tipos: Set<ExerciseType> =
            guardados?.mapNotNull { ExerciseType.desdeClave(it) }?.toSet() ?: emptySet()
        return if (tipos.isEmpty()) ExerciseType.values().toSet() else tipos
    }

    fun setTipos(context: Context, tipos: Set<ExerciseType>) {
        if (tipos.isEmpty()) return
        prefs(context).edit()
            .putStringSet(KEY_TIPOS, tipos.map { it.clave }.toSet())
            .apply()
    }

    fun getVoz(context: Context): Boolean = prefs(context).getBoolean("voz", false)

    fun setVoz(context: Context, activa: Boolean) {
        prefs(context).edit().putBoolean("voz", activa).apply()
    }

    /** PIN de padres. Vacío = sin PIN. */
    fun getPin(context: Context): String = prefs(context).getString("pin", "") ?: ""

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString("pin", pin).apply()
    }

    /** 75 -> "1 min 15 s" */
    fun formatear(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m == 0 -> "$s s"
            s == 0 -> "$m min"
            else -> "$m min $s s"
        }
    }
}
