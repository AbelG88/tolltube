package com.peajecognitivo.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class PeajeAccessibilityService : AccessibilityService() {

    companion object {
        var isYouTubeActive = false
            private set

        /** true entre onServiceConnected y onUnbind/onDestroy. Lo lee el panel de estado. */
        var conectado = false
            private set

        /** Último paquete que generó un cambio de ventana. Lo lee el panel de estado. */
        var ultimoPaquete = ""
            private set

        private val YOUTUBE_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.google.android.apps.youtube.kids",
            "com.google.android.apps.youtube.mango",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        conectado = true
        PeajeLog.d("SERVICIO conectado (isYouTubeActive=$isYouTubeActive)")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        conectado = false
        PeajeLog.d("SERVICIO desvinculado (onUnbind)")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        conectado = false
        PeajeLog.d("SERVICIO destruido (onDestroy)")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            ultimoPaquete = packageName

            val wasActive = isYouTubeActive
            isYouTubeActive = packageName in YOUTUBE_PACKAGES

            PeajeLog.d(
                "EVENTO ventana pkg=$packageName clase=${event.className} " +
                    "youtube: $wasActive -> $isYouTubeActive"
            )

            if (isYouTubeActive && !wasActive) {
                PeajeLog.d("-> entró a YouTube: startTimer")
                ExerciseTimerManager.startTimer(this)
            } else if (!isYouTubeActive && wasActive) {
                PeajeLog.d("-> salió de YouTube (por pkg=$packageName): pauseTimer")
                ExerciseTimerManager.pauseTimer(this)
            }
        }
    }

    override fun onInterrupt() {
        PeajeLog.d("SERVICIO onInterrupt")
    }
}
