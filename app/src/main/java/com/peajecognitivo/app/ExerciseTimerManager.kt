package com.peajecognitivo.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Temporizador interno del peaje.
 *
 * Antes usaba AlarmManager.setRepeating, que es inexacta y Android no permite
 * por debajo de 60 s. Ahora usa un Handler en el hilo principal: es exacto y
 * admite cualquier intervalo en segundos (ver SettingsStore).
 *
 * Mientras YouTube esté activo, cada vez que se cumple el tiempo se muestra el overlay.
 * La API pública (startTimer / pauseTimer) es la misma de antes.
 */
class ExerciseTimerManager {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var running = false
        private var appContext: Context? = null

        // Solo diagnóstico: cuándo debía sonar el próximo tick (elapsedRealtime)
        private var proximaEsperada = 0L

        private val tick = object : Runnable {
            override fun run() {
                val ctx = appContext ?: return
                val retraso = SystemClock.elapsedRealtime() - proximaEsperada
                PeajeLog.d(
                    "TIMER sonó. isYouTubeActive=${PeajeAccessibilityService.isYouTubeActive} " +
                        "retraso_vs_esperado=${retraso}ms"
                )
                if (PeajeAccessibilityService.isYouTubeActive) {
                    PeajeOverlayManager.show(ctx)
                } else {
                    PeajeLog.d("TIMER ignorado: no se cree que YouTube esté en primer plano")
                }
                if (running) programar(ctx)
            }
        }

        private fun programar(ctx: Context) {
            val intervalo = SettingsStore.getIntervalMs(ctx)
            proximaEsperada = SystemClock.elapsedRealtime() + intervalo
            handler.postDelayed(tick, intervalo)
            PeajeLog.d("TIMER programado: próximo en ${intervalo / 1000}s")
        }

        fun startTimer(context: Context) {
            if (running) {
                PeajeLog.d("TIMER startTimer ignorado: ya había un timer activo")
                return
            }
            running = true
            appContext = context.applicationContext
            programar(appContext!!)
        }

        fun pauseTimer(context: Context) {
            if (!running) {
                PeajeLog.d("TIMER pauseTimer: no había timer activo")
                return
            }
            handler.removeCallbacks(tick)
            running = false
            PeajeLog.d("TIMER cancelado")
        }

        /** Se llama al guardar un tiempo nuevo: si hay un timer corriendo, lo reprograma ya. */
        fun reprogramar(context: Context) {
            if (!running) return
            handler.removeCallbacks(tick)
            programar(context.applicationContext)
            PeajeLog.d("TIMER reprogramado por cambio de ajustes")
        }
    }
}
