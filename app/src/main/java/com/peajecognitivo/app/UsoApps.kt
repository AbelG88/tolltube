package com.peajecognitivo.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Detección de la app en primer plano con UsageStatsManager (sin servicio de accesibilidad).
 * Necesita el acceso especial "Acceso a datos de uso" (Ajustes > Apps > Acceso especial).
 */
object UsoApps {
    /** Último paquete que pasó al primer plano según los eventos de uso. Lo lee el panel de estado. */
    var ultimoPaquete: String? = null
        private set

    private var actual: String? = null
    private var ultimaConsulta = 0L
    private var avisoPermisoLogueado = false

    fun tienePermiso(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return modo == AppOpsManager.MODE_ALLOWED
    }

    /** Lee los eventos nuevos y actualiza cuál es la app en primer plano. */
    fun actualizar(context: Context) {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val ahora = System.currentTimeMillis()
            // La primera vez se mira 2 horas atrás; después, desde la última consulta con un poco de solape
            val desde = if (ultimaConsulta == 0L) ahora - 2 * 60 * 60 * 1000L else ultimaConsulta - 3000L
            val eventos = usm.queryEvents(desde, ahora)
            val e = UsageEvents.Event()
            val anterior = actual
            while (eventos.hasNextEvent()) {
                eventos.getNextEvent(e)
                when (e.eventType) {
                    EVENTO_PRIMER_PLANO -> actual = e.packageName
                    EVENTO_SEGUNDO_PLANO -> if (actual == e.packageName) actual = null
                    EVENTO_PANTALLA_APAGADA -> actual = null
                }
            }
            ultimaConsulta = ahora
            if (actual != anterior) {
                if (actual != null) ultimoPaquete = actual
                PeajeLog.d("USO app en primer plano: $actual")
            }
        } catch (ex: SecurityException) {
            if (!avisoPermisoLogueado) {
                avisoPermisoLogueado = true
                PeajeLog.d("USO sin permiso de acceso a datos de uso: ${ex.message}")
            }
        } catch (ex: Exception) {
            PeajeLog.d("USO error al consultar: ${ex.javaClass.simpleName}: ${ex.message}")
        }
    }

    /** Misma clasificación que usaba la accesibilidad: YouTube / otra app / incierto. */
    fun clasificar(context: Context): Pair<Pantalla, String?> {
        val paquete = actual ?: return Pair(Pantalla.INCIERTO, null)
        val pantalla = when {
            paquete in PeajeAccessibilityService.YOUTUBE_PACKAGES -> Pantalla.YOUTUBE
            esRuido(context, paquete) -> Pantalla.INCIERTO
            else -> Pantalla.OTRA_APP
        }
        return Pair(pantalla, paquete)
    }

    private fun esRuido(context: Context, paquete: String): Boolean {
        if (paquete == "com.android.systemui") return true
        if (paquete.contains("inputmethod")) return true
        if (paquete == context.packageName) return true
        val teclado = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        } catch (e: Exception) {
            null
        }
        return teclado != null && teclado.substringBefore('/') == paquete
    }

    // Valores de UsageEvents.Event (ACTIVITY_RESUMED=1 / ACTIVITY_PAUSED=2 / SCREEN_NON_INTERACTIVE=16)
    private const val EVENTO_PRIMER_PLANO = 1
    private const val EVENTO_SEGUNDO_PLANO = 2
    private const val EVENTO_PANTALLA_APAGADA = 16
}
