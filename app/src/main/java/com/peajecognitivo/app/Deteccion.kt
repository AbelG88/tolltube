package com.peajecognitivo.app

import android.content.Context

/** Qué hay realmente en pantalla en este momento. */
enum class Pantalla {
    /** La app en primer plano es una de las vigiladas (YouTube, YouTube Kids, TikTok, Instagram). */
    VIGILADA,

    /** La app en primer plano es otra (launcher, WhatsApp, etc.). */
    OTRA_APP,

    /** No se sabe: hay una ventana "de ruido" (teclado, barra del sistema, nuestro overlay) o no se pudo leer. */
    INCIERTO
}

/** Apps que el padre puede vigilar. Cada una agrupa los paquetes de sus variantes. */
enum class AppVigilada(val clave: String, val nombre: String, val porDefecto: Boolean, val paquetes: Set<String>) {
    YOUTUBE(
        "youtube", "YouTube", true,
        setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.google.android.apps.youtube.mango",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube"
        )
    ),
    YOUTUBE_KIDS("youtube_kids", "YouTube Kids", true, setOf("com.google.android.apps.youtube.kids")),
    TIKTOK(
        "tiktok", "TikTok", false,
        setOf("com.zhiliaoapp.musically", "com.zhiliaoapp.musically.go", "com.ss.android.ugc.trill")
    ),
    INSTAGRAM("instagram", "Instagram", false, setOf("com.instagram.android", "com.instagram.lite"))
}

/** Detección de las apps vigiladas mediante el acceso a datos de uso (UsageStatsManager). */
object Deteccion {

    /** Paquetes de todas las apps que el padre tiene marcadas como vigiladas. */
    fun paquetesVigilados(context: Context): Set<String> =
        AppVigilada.values()
            .filter { SettingsStore.appVigilada(context, it) }
            .flatMap { it.paquetes }
            .toSet()

    /** Cómo anotar una app en el registro: solo si es vigilada o no; nunca el nombre del paquete. */
    fun etiqueta(context: Context, paquete: String?): String = when {
        paquete == null -> "ninguna"
        paquete in paquetesVigilados(context) -> "vigilada"
        else -> "otra"
    }

    /** Qué hay en pantalla ahora. Se llama en cada ciclo del timer. */
    fun clasificar(context: Context): Pair<Pantalla, String?> {
        UsoApps.actualizar(context)
        return UsoApps.clasificar(context)
    }
}
