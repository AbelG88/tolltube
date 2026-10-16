package com.peajecognitivo.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Temporizador interno del peaje.
 *
 * Cómo funciona (desde la 1.3):
 *  - Se ARRANCA cuando el servicio de accesibilidad ve aparecer YouTube.
 *  - Los eventos de otras ventanas (teclado, barra del sistema, nuestro overlay) ya NO lo apagan.
 *  - Cada vez que se cumple el tiempo, se pregunta al sistema qué app está realmente en pantalla:
 *      · YouTube  -> se muestra el ejercicio y el timer espera a que se resuelva;
 *                    al resolverse, se vuelve a contar el tiempo completo.
 *      · otra app -> el timer se detiene (se reactiva solo al volver a YouTube).
 *      · incierto -> (teclado, barra, no se pudo leer) se saltea este ciclo y se sigue contando.
 */
class ExerciseTimerManager {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var running = false
        private var esperandoCierre = false
        private var appContext: Context? = null

        // Cuándo debía sonar el próximo ciclo (elapsedRealtime). Sirve para medir retrasos y para el panel.
        private var proximaEsperada = 0L

        private val tick = object : Runnable {
            override fun run() {
                alSonar()
            }
        }

        private fun alSonar() {
            val ctx = appContext ?: return
            if (!running) return
            if (!SettingsStore.getActivo(ctx)) {
                running = false
                esperandoCierre = false
                PeajeLog.d("TIMER detenido: el peaje está apagado en los ajustes")
                return
            }

            val retraso = SystemClock.elapsedRealtime() - proximaEsperada
            val (pantalla, paquete) = PeajeAccessibilityService.clasificarPantalla()
            PeajeLog.d(
                "TIMER sonó. en_pantalla=$paquete -> $pantalla retraso_vs_esperado=${retraso}ms"
            )

            when (pantalla) {
                Pantalla.YOUTUBE -> {
                    PeajeOverlayManager.show(ctx)
                    // No se programa otro ciclo: se espera a que se resuelva el ejercicio
                    esperandoCierre = true
                    PeajeLog.d("TIMER en espera hasta que se resuelva el ejercicio")
                }
                Pantalla.OTRA_APP -> {
                    running = false
                    PeajeLog.d("TIMER detenido: el usuario está en $paquete. Se reactiva al volver a YouTube")
                }
                Pantalla.INCIERTO -> {
                    PeajeLog.d("TIMER ciclo salteado: pantalla incierta, se sigue contando")
                    programar(ctx)
                }
            }
        }

        private fun programar(ctx: Context) {
            val intervalo = SettingsStore.getIntervalMs(ctx)
            esperandoCierre = false
            proximaEsperada = SystemClock.elapsedRealtime() + intervalo
            handler.postDelayed(tick, intervalo)
            PeajeLog.d("TIMER programado: próximo en ${intervalo / 1000}s")
        }

        fun startTimer(context: Context) {
            if (running) return   // ya corriendo: no se anota para no llenar el registro
            if (!SettingsStore.getActivo(context)) return
            running = true
            appContext = context.applicationContext
            programar(appContext!!)
        }

        /** Lo llama el overlay cuando el ejercicio se resolvió: se vuelve a contar el tiempo completo. */
        fun alCerrarEjercicio(context: Context) {
            if (!running) return
            handler.removeCallbacks(tick)
            programar(appContext ?: context.applicationContext)
            PeajeLog.d("TIMER reiniciado tras resolver el ejercicio")
        }

        /** Para el timer por completo (interruptor apagado). */
        fun detener() {
            handler.removeCallbacks(tick)
            running = false
            esperandoCierre = false
            PeajeLog.d("TIMER detenido por el interruptor")
        }

        /** Se llama al guardar un tiempo nuevo: si hay un ciclo en curso, se reprograma ya. */
        fun reprogramar(context: Context) {
            if (!running || esperandoCierre) return
            handler.removeCallbacks(tick)
            programar(context.applicationContext)
            PeajeLog.d("TIMER reprogramado por cambio de ajustes")
        }

        /** Para el panel de estado: "corriendo, próximo en 12 s", "esperando ejercicio" o "detenido". */
        fun descripcionEstado(): String = when {
            !running -> "detenido (se activa al abrir YouTube)"
            esperandoCierre -> "esperando que se resuelva el ejercicio"
            else -> {
                val faltan = ((proximaEsperada - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)
                "corriendo, próximo ejercicio en ${faltan}s"
            }
        }
    }
}
