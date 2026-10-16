package com.peajecognitivo.app

import android.content.Context

/** Elige cómo se detecta YouTube: accesibilidad (anterior) o uso de apps (nuevo). */
object Deteccion {
    const val ACCESIBILIDAD = "acc"
    const val USO = "uso"

    fun modo(context: Context): String = SettingsStore.getDeteccion(context)

    fun usaAccesibilidad(context: Context): Boolean = modo(context) == ACCESIBILIDAD

    /** Qué hay en pantalla ahora, según el modo elegido. Se llama en cada ciclo del timer. */
    fun clasificar(context: Context): Pair<Pantalla, String?> =
        if (modo(context) == USO) {
            UsoApps.actualizar(context)
            UsoApps.clasificar(context)
        } else {
            PeajeAccessibilityService.clasificarPantalla()
        }
}
