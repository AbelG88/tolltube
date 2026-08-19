package com.peajecognitivo.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PeajeAccessibilityService : AccessibilityService() {

    companion object {
        var isYouTubeActive = false
            private set
        private val YOUTUBE_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Servicio iniciado correctamente
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            val wasActive = isYouTubeActive
            isYouTubeActive = packageName in YOUTUBE_PACKAGES

            if (isYouTubeActive && !wasActive) {
                ExerciseTimerManager.startTimer(this)
            } else if (!isYouTubeActive && wasActive) {
                ExerciseTimerManager.pauseTimer(this)
            }
        }
    }

    override fun onInterrupt() {}
}