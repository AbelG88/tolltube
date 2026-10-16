package com.peajecognitivo.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/** Qué hay realmente en pantalla en este momento. */
enum class Pantalla {
    /** La app en primer plano es YouTube. */
    YOUTUBE,

    /** La app en primer plano es otra (launcher, WhatsApp, etc.). */
    OTRA_APP,

    /** No se sabe: hay una ventana "de ruido" (teclado, barra del sistema, nuestro overlay) o no se pudo leer. */
    INCIERTO
}

class PeajeAccessibilityService : AccessibilityService() {

    companion object {
        /** Última impresión basada en eventos. Solo informativa (panel de estado). */
        var isYouTubeActive = false
            private set

        /** true entre onServiceConnected y onUnbind/onDestroy. Lo lee el panel de estado. */
        var conectado = false
            private set

        /** Último paquete que generó un cambio de ventana. Lo lee el panel de estado. */
        var ultimoPaquete = ""
            private set

        private var instancia: PeajeAccessibilityService? = null

        val YOUTUBE_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.google.android.apps.youtube.kids",
            "com.google.android.apps.youtube.mango",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube"
        )

        /** Paquete de la ventana activa AHORA, consultándolo al sistema (no por eventos). */
        @Suppress("DEPRECATION")
        fun paqueteEnPrimerPlano(): String? {
            val servicio = instancia ?: return null
            val raiz = try {
                servicio.rootInActiveWindow
            } catch (e: Exception) {
                null
            }
            if (raiz == null) return null
            val paquete = raiz.packageName?.toString()
            raiz.recycle()
            return paquete
        }

        /** Clasifica la pantalla real. Se usa en cada ciclo del timer. */
        fun clasificarPantalla(): Pair<Pantalla, String?> {
            val paquete = paqueteEnPrimerPlano() ?: return Pair(Pantalla.INCIERTO, null)
            val pantalla = when {
                paquete in YOUTUBE_PACKAGES -> Pantalla.YOUTUBE
                esRuido(paquete) -> Pantalla.INCIERTO
                else -> Pantalla.OTRA_APP
            }
            return Pair(pantalla, paquete)
        }

        /**
         * Ventanas que no significan "el usuario cambió de app": la barra del sistema,
         * el teclado y nuestro propio overlay/app.
         */
        private fun esRuido(paquete: String): Boolean {
            if (paquete == "com.android.systemui") return true
            if (paquete.contains("inputmethod")) return true
            val servicio = instancia ?: return false
            if (paquete == servicio.packageName) return true
            val teclado = try {
                Settings.Secure.getString(
                    servicio.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                )
            } catch (e: Exception) {
                null
            }
            return teclado != null && teclado.substringBefore('/') == paquete
        }
    }

    private var ultimoLogueado = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instancia = this
        conectado = true
        PeajeLog.d("SERVICIO conectado")
        // Si YouTube ya estaba abierto antes de conectar, no llega ningún evento: se mira la pantalla.
        revisarPantallaInicial()
        Handler(Looper.getMainLooper()).postDelayed({ revisarPantallaInicial() }, 1500)
    }

    private fun revisarPantallaInicial() {
        val (pantalla, paquete) = clasificarPantalla()
        PeajeLog.d("SERVICIO revisó la pantalla: en_pantalla=$paquete -> $pantalla")
        if (pantalla == Pantalla.YOUTUBE && Deteccion.usaAccesibilidad(this)) {
            isYouTubeActive = true
            ExerciseTimerManager.startTimer(this)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        conectado = false
        if (instancia === this) instancia = null
        PeajeLog.d("SERVICIO desvinculado (onUnbind)")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        conectado = false
        if (instancia === this) instancia = null
        PeajeLog.d("SERVICIO destruido (onDestroy)")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquete = event.packageName?.toString() ?: return
        ultimoPaquete = paquete

        val ruido = esRuido(paquete)
        val enYoutube = paquete in YOUTUBE_PACKAGES
        if (!ruido) isYouTubeActive = enYoutube

        // Para no llenar el registro, las ventanas de ruido repetidas se anotan una sola vez seguida
        if (!ruido || paquete != ultimoLogueado) {
            val detalle = if (ruido) "(ruido: no cambia nada)" else "youtube=$isYouTubeActive"
            PeajeLog.d("EVENTO ventana pkg=$paquete clase=${event.className} $detalle")
            ultimoLogueado = paquete
        }

        // Los eventos ya NO apagan el timer. Solo lo arrancan cuando aparece YouTube.
        // Quién está realmente en pantalla se decide en cada ciclo del timer.
        if (enYoutube && Deteccion.usaAccesibilidad(this)) {
            ExerciseTimerManager.startTimer(this)
        }
    }

    override fun onInterrupt() {
        PeajeLog.d("SERVICIO onInterrupt")
    }
}
