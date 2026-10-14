package com.peajecognitivo.app

import android.content.Context

/**
 * Ajustes guardados en el celular (SharedPreferences).
 * Por ahora: el tiempo entre ejercicios, guardado en segundos.
 */
object SettingsStore {
    private const val PREFS = "peaje_prefs"
    private const val KEY_INTERVAL_SECONDS = "interval_seconds"

    const val DEFAULT_SECONDS = 60          // 1 minuto (igual que antes)
    const val MIN_SECONDS = 10              // por debajo de esto es incómodo
    const val MAX_SECONDS = 3 * 60 * 60     // 3 horas

    fun getIntervalSeconds(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL_SECONDS, DEFAULT_SECONDS).coerceIn(MIN_SECONDS, MAX_SECONDS)
    }

    fun getIntervalMs(context: Context): Long = getIntervalSeconds(context) * 1000L

    fun setIntervalSeconds(context: Context, seconds: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_SECONDS, seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
            .apply()
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
