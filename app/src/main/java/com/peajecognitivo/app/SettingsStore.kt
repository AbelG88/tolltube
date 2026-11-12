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

    /** Clave de un ajuste del niño activo. */
    private fun k(context: Context, key: String) = Ninos.clave(context, key)

    fun getIntervalSeconds(context: Context): Int =
        prefs(context).getInt(k(context, KEY_INTERVAL_SECONDS), DEFAULT_SECONDS)
            .coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun getIntervalMs(context: Context): Long = getIntervalSeconds(context) * 1000L

    fun setIntervalSeconds(context: Context, seconds: Int) {
        prefs(context).edit()
            .putInt(k(context, KEY_INTERVAL_SECONDS), seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
            .apply()
    }

    /** Tipos de fábrica habilitados. Sin nada guardado están todos; puede quedar vacío si hay módulos activos. */
    fun getTipos(context: Context): Set<ExerciseType> {
        val fabrica = ExerciseType.values().filter { it != ExerciseType.MODULOS }.toSet()
        val guardados = prefs(context).getStringSet(k(context, KEY_TIPOS), null) ?: return fabrica
        return guardados.mapNotNull { ExerciseType.desdeClave(it) }.filter { it in fabrica }.toSet()
    }

    fun setTipos(context: Context, tipos: Set<ExerciseType>) {
        prefs(context).edit()
            .putStringSet(k(context, KEY_TIPOS), tipos.map { it.clave }.toSet())
            .apply()
    }

    fun getVoz(context: Context): Boolean = prefs(context).getBoolean("voz", false)

    fun setVoz(context: Context, activa: Boolean) {
        prefs(context).edit().putBoolean("voz", activa).apply()
    }

    // El PIN de padres está en PinStore (hash + sal, con bloqueo por intentos).

    /** Cómo se detecta YouTube: "uso" (uso de apps, por defecto) o "acc" (accesibilidad, alternativo). */
    /** Si está activo, el tiempo solo corre mientras YouTube está en pantalla Y hay audio sonando. */
    fun getSoloReproduciendo(context: Context): Boolean = prefs(context).getBoolean("solo_reproduciendo", true)

    fun setSoloReproduciendo(context: Context, v: Boolean) {
        prefs(context).edit().putBoolean("solo_reproduciendo", v).apply()
    }

    /** Si una app (YouTube, TikTok, etc.) está marcada para vigilarse. */
    fun appVigilada(context: Context, app: AppVigilada): Boolean =
        prefs(context).getBoolean(k(context, "vigilar_${app.clave}"), app.porDefecto)

    fun setAppVigilada(context: Context, app: AppVigilada, v: Boolean) {
        prefs(context).edit().putBoolean(k(context, "vigilar_${app.clave}"), v).apply()
    }

    /** Interruptor general. Apagado: el peaje no aparece nunca (solo el botón de prueba). */
    fun getActivo(context: Context): Boolean = prefs(context).getBoolean("activo", true)

    fun setActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean("activo", activo).apply()
    }

    /** Cuántos ejercicios seguidos hay que resolver cada vez que suena el timer. */
    const val MAX_SERIE = 20

    fun getSerie(context: Context): Int = prefs(context).getInt(k(context, "serie"), 1).coerceIn(1, MAX_SERIE)

    fun setSerie(context: Context, n: Int) {
        prefs(context).edit().putInt(k(context, "serie"), n.coerceIn(1, MAX_SERIE)).apply()
    }

    /** Nivel de dificultad: 1 fácil, 2 medio (por defecto), 3 difícil. */
    fun getNivel(context: Context): Int = prefs(context).getInt(k(context, "nivel"), 2).coerceIn(1, 3)

    fun setNivel(context: Context, nivel: Int) {
        prefs(context).edit().putInt(k(context, "nivel"), nivel.coerceIn(1, 3)).apply()
    }

    /** Historial acumulado por tipo: ejercicios resueltos y errores cometidos. */
    fun registrarResultado(context: Context, tipo: ExerciseType, errores: Int) {
        val p = prefs(context)
        val ok = k(context, "hist_${tipo.clave}_ok")
        val err = k(context, "hist_${tipo.clave}_err")
        p.edit().putInt(ok, p.getInt(ok, 0) + 1).putInt(err, p.getInt(err, 0) + errores).apply()
    }

    fun registrarError(context: Context, tipo: ExerciseType) {
        val p = prefs(context)
        val err = k(context, "hist_${tipo.clave}_err")
        p.edit().putInt(err, p.getInt(err, 0) + 1).apply()
    }

    fun historial(context: Context, tipo: ExerciseType): Pair<Int, Int> {
        val p = prefs(context)
        return Pair(p.getInt(k(context, "hist_${tipo.clave}_ok"), 0), p.getInt(k(context, "hist_${tipo.clave}_err"), 0))
    }

    fun borrarHistorial(context: Context) {
        val e = prefs(context).edit()
        ExerciseType.values().forEach {
            e.remove(k(context, "hist_${it.clave}_ok")).remove(k(context, "hist_${it.clave}_err"))
        }
        e.apply()
    }

    /** Aciertos de hoy (se reinicia solo cada día). */
    fun aciertosHoy(context: Context): Int {
        val p = prefs(context)
        return if (p.getString(k(context, "aciertos_dia"), "") == hoy()) p.getInt(k(context, "aciertos_n"), 0) else 0
    }

    /** Suma un acierto a hoy y devuelve el total del día. */
    fun registrarAcierto(context: Context): Int {
        val n = aciertosHoy(context) + 1
        prefs(context).edit().putString(k(context, "aciertos_dia"), hoy()).putInt(k(context, "aciertos_n"), n)
            .putInt("aciertos_total", aciertosTotal(context) + 1).apply()
        return n
    }

    /**
     * Aciertos de todos los niños desde siempre (sirve para decidir cuándo pedir apoyo).
     * En instalaciones anteriores arranca con lo que ya había en el progreso.
     */
    fun aciertosTotal(context: Context): Int {
        val p = prefs(context)
        if (p.contains("aciertos_total")) return p.getInt("aciertos_total", 0)
        val previo = p.all.filter { (k, v) -> v is Int && Regex("(n\\d+_)?hist_.*_ok").matches(k) }.values.sumOf { it as Int }
        p.edit().putInt("aciertos_total", previo).apply()
        return previo
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

    fun getNombreNino(context: Context): String = prefs(context).getString(k(context, "nombre_nino"), "") ?: ""

    fun setNombreNino(context: Context, nombre: String) {
        prefs(context).edit().putString(k(context, "nombre_nino"), nombre.trim().take(30)).apply()
    }

    /** Instalaciones anteriores (ya configuradas) no vuelven a ver el asistente. */
    fun onboardingHecho(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean("onboarding_hecho", p.contains("interval_seconds") || p.contains("pin_hash") || p.contains("pin"))
    }

    fun setOnboardingHecho(context: Context, v: Boolean) {
        prefs(context).edit().putBoolean("onboarding_hecho", v).apply()
    }
}
