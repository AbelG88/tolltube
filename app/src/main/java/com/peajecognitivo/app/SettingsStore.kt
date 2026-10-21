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

    /** Tipos de fábrica habilitados. Sin nada guardado están todos; puede quedar vacío si hay módulos activos. */
    fun getTipos(context: Context): Set<ExerciseType> {
        val fabrica = ExerciseType.values().filter { it != ExerciseType.MODULOS }.toSet()
        val guardados = prefs(context).getStringSet(KEY_TIPOS, null) ?: return fabrica
        return guardados.mapNotNull { ExerciseType.desdeClave(it) }.filter { it in fabrica }.toSet()
    }

    fun setTipos(context: Context, tipos: Set<ExerciseType>) {
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

    /** Cómo se detecta YouTube: "uso" (uso de apps, por defecto) o "acc" (accesibilidad, alternativo). */
    /** Si está activo, el tiempo solo corre mientras YouTube está en pantalla Y hay audio sonando. */
    fun getSoloReproduciendo(context: Context): Boolean = prefs(context).getBoolean("solo_reproduciendo", true)

    fun setSoloReproduciendo(context: Context, v: Boolean) {
        prefs(context).edit().putBoolean("solo_reproduciendo", v).apply()
    }

    /** Interruptor general. Apagado: el peaje no aparece nunca (solo el botón de prueba). */
    fun getActivo(context: Context): Boolean = prefs(context).getBoolean("activo", true)

    fun setActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean("activo", activo).apply()
    }

    /** Cuántos ejercicios seguidos hay que resolver cada vez que suena el timer. */
    const val MAX_SERIE = 20

    fun getSerie(context: Context): Int = prefs(context).getInt("serie", 1).coerceIn(1, MAX_SERIE)

    fun setSerie(context: Context, n: Int) {
        prefs(context).edit().putInt("serie", n.coerceIn(1, MAX_SERIE)).apply()
    }

    /** Nivel de dificultad: 1 fácil, 2 medio (por defecto), 3 difícil. */
    fun getNivel(context: Context): Int = prefs(context).getInt("nivel", 2).coerceIn(1, 3)

    fun setNivel(context: Context, nivel: Int) {
        prefs(context).edit().putInt("nivel", nivel.coerceIn(1, 3)).apply()
    }

    /** Historial acumulado por tipo: ejercicios resueltos y errores cometidos. */
    fun registrarResultado(context: Context, tipo: ExerciseType, errores: Int) {
        val p = prefs(context)
        p.edit()
            .putInt("hist_${tipo.clave}_ok", p.getInt("hist_${tipo.clave}_ok", 0) + 1)
            .putInt("hist_${tipo.clave}_err", p.getInt("hist_${tipo.clave}_err", 0) + errores)
            .apply()
    }

    fun registrarError(context: Context, tipo: ExerciseType) {
        val p = prefs(context)
        p.edit().putInt("hist_${tipo.clave}_err", p.getInt("hist_${tipo.clave}_err", 0) + 1).apply()
    }

    fun historial(context: Context, tipo: ExerciseType): Pair<Int, Int> {
        val p = prefs(context)
        return Pair(p.getInt("hist_${tipo.clave}_ok", 0), p.getInt("hist_${tipo.clave}_err", 0))
    }

    fun borrarHistorial(context: Context) {
        val e = prefs(context).edit()
        ExerciseType.values().forEach {
            e.remove("hist_${it.clave}_ok").remove("hist_${it.clave}_err")
        }
        e.apply()
    }

    /** Aciertos de hoy (se reinicia solo cada día). */
    fun aciertosHoy(context: Context): Int {
        val p = prefs(context)
        return if (p.getString("aciertos_dia", "") == hoy()) p.getInt("aciertos_n", 0) else 0
    }

    /** Suma un acierto a hoy y devuelve el total del día. */
    fun registrarAcierto(context: Context): Int {
        val n = aciertosHoy(context) + 1
        prefs(context).edit().putString("aciertos_dia", hoy()).putInt("aciertos_n", n).apply()
        return n
    }

    private fun hoy(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

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
