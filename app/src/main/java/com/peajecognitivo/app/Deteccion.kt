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
    INSTAGRAM("instagram", "Instagram", false, setOf("com.instagram.android", "com.instagram.lite")),
    NETFLIX("netflix", "Netflix", false, setOf("com.netflix.mediaclient"))
}

/** Cómo se decide qué apps piden ejercicio. */
enum class ModoApps(val clave: String) {
    /** Las apps de siempre (YouTube, TikTok, Instagram, Netflix), según lo marcado. */
    SIEMPRE("siempre"),

    /** Solo las que el padre eligió de la lista de apps instaladas. */
    ELEGIR("elegir"),

    /** Todas piden ejercicio, salvo las que el padre deja libres. */
    TODO("todo");

    companion object {
        fun desdeClave(c: String?) = values().firstOrNull { it.clave == c } ?: SIEMPRE
    }
}

/** Detección de las apps vigiladas mediante el acceso a datos de uso (UsageStatsManager). */
object Deteccion {

    /** Paquetes de las apps de siempre que el padre tiene marcadas. */
    private fun paquetesDeSiempre(context: Context): Set<String> =
        AppVigilada.values()
            .filter { SettingsStore.appVigilada(context, it) }
            .flatMap { it.paquetes }
            .toSet()

    /** Apps que nunca piden ejercicio en el modo "todo" (teléfono, ajustes, instalador y similares). */
    private val SIEMPRE_LIBRES = setOf(
        "android", "com.android.settings", "com.android.phone", "com.android.server.telecom",
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller", "com.android.packageinstaller"
    )

    /** Apps que ya vienen libres al elegir "todo menos lo que permito". */
    val LIBRES_POR_DEFECTO = setOf(
        "com.whatsapp", "com.google.android.apps.maps", "com.google.android.apps.messaging",
        "com.android.mms", "com.samsung.android.messaging", "com.android.contacts",
        "com.google.android.contacts", "com.samsung.android.app.contacts"
    )

    /** ¿Esta app pide ejercicio? Depende del modo elegido para el niño activo. */
    fun esVigilada(context: Context, paquete: String): Boolean = when (SettingsStore.getModoApps(context)) {
        ModoApps.SIEMPRE -> paquete in paquetesDeSiempre(context)
        ModoApps.ELEGIR -> paquete in SettingsStore.getAppsElegidas(context)
        ModoApps.TODO ->
            paquete !in SIEMPRE_LIBRES &&
                paquete !in SettingsStore.getAppsLibres(context) &&
                paquete !in AppsInstaladas.launchers(context) &&
                !UsoApps.esRuido(context, paquete)
    }

    /** Cómo anotar una app en el registro: solo si es vigilada o no; nunca el nombre del paquete. */
    fun etiqueta(context: Context, paquete: String?): String = when {
        paquete == null -> "ninguna"
        esVigilada(context, paquete) -> "vigilada"
        else -> "otra"
    }

    /** Qué hay en pantalla ahora. Se llama en cada ciclo del timer. */
    fun clasificar(context: Context): Pair<Pantalla, String?> {
        UsoApps.actualizar(context)
        return UsoApps.clasificar(context)
    }
}
