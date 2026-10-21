package com.peajecognitivo.app

import android.content.Context

/** Qué hay realmente en pantalla en este momento. */
enum class Pantalla {
    /** La app en primer plano es YouTube. */
    YOUTUBE,

    /** La app en primer plano es otra (launcher, WhatsApp, etc.). */
    OTRA_APP,

    /** No se sabe: hay una ventana "de ruido" (teclado, barra del sistema, nuestro overlay) o no se pudo leer. */
    INCIERTO
}

/** Detección de YouTube mediante el acceso a datos de uso (UsageStatsManager). */
object Deteccion {

    val YOUTUBE_PACKAGES = setOf(
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.mango",
        "app.revanced.android.youtube",
        "com.vanced.android.youtube"
    )

    /** Qué hay en pantalla ahora. Se llama en cada ciclo del timer. */
    fun clasificar(context: Context): Pair<Pantalla, String?> {
        UsoApps.actualizar(context)
        return UsoApps.clasificar(context)
    }
}
