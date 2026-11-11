package com.peajecognitivo.app

import android.content.Context

/**
 * Niños cargados en la app. Cada uno tiene sus propios ajustes y progreso; el padre elige
 * desde el panel cuál está activo (el que usa el celular ahora).
 *
 * El primer niño ("1") conserva las claves de siempre, así las instalaciones anteriores
 * siguen funcionando sin migrar nada. Los demás guardan sus claves con el prefijo "n<id>_".
 */
object Ninos {
    private const val PREFS = "peaje_prefs"
    private const val K_IDS = "ninos_ids"
    private const val K_ACTIVO = "nino_activo"
    private const val K_SIG = "ninos_sig"
    const val MAX = 6

    private val EXACTAS = setOf("interval_seconds", "tipos_ejercicio", "serie", "nivel", "nombre_nino", "aciertos_dia", "aciertos_n")
    private val PREFIJOS = listOf("vigilar_", "hist_", "modulo_on_")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ids(context: Context): List<String> =
        (prefs(context).getString(K_IDS, "1") ?: "1").split(",").filter { it.isNotEmpty() }.ifEmpty { listOf("1") }

    fun activo(context: Context): String {
        val a = prefs(context).getString(K_ACTIVO, null)
        val ids = ids(context)
        return if (a != null && a in ids) a else ids.first()
    }

    fun setActivo(context: Context, id: String) {
        if (id in ids(context)) prefs(context).edit().putString(K_ACTIVO, id).apply()
    }

    fun clave(id: String, key: String): String = if (id == "1") key else "n${id}_$key"

    /** Clave de un ajuste del niño activo. */
    fun clave(context: Context, key: String): String = clave(activo(context), key)

    fun nombreDe(context: Context, id: String): String =
        prefs(context).getString(clave(id, "nombre_nino"), "")?.takeIf { it.isNotEmpty() }
            ?: "Niño ${ids(context).indexOf(id) + 1}"

    /** Crea un niño nuevo (sin hacerlo activo) y devuelve su id. */
    fun crear(context: Context): String {
        val p = prefs(context)
        val id = p.getInt(K_SIG, 2).toString()
        p.edit()
            .putString(K_IDS, (ids(context) + id).joinToString(","))
            .putInt(K_SIG, id.toInt() + 1)
            .apply()
        return id
    }

    /** Quita un niño y todo lo suyo. El último que queda no se puede quitar. */
    fun quitar(context: Context, id: String) {
        val restantes = ids(context) - id
        if (restantes.isEmpty() || restantes.size == ids(context).size) return
        val p = prefs(context)
        val e = p.edit()
        p.all.keys.forEach { k ->
            val suyo = if (id == "1") k in EXACTAS || PREFIJOS.any { k.startsWith(it) } else k.startsWith("n${id}_")
            if (suyo) e.remove(k)
        }
        e.putString(K_IDS, restantes.joinToString(","))
        if (activo(context) == id) e.putString(K_ACTIVO, restantes.first())
        e.apply()
    }
}
