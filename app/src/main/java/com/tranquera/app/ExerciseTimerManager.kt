package com.tranquera.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Temporizador interno de la tranquera.
 *
 * Cómo funciona (desde la 1.3):
 *  - Se ARRANCA cuando el monitor de uso de apps ve aparecer YouTube.
 *  - Los eventos de otras ventanas (teclado, barra del sistema, nuestro overlay) ya NO lo apagan.
 *  - Cada vez que se cumple el tiempo, se pregunta al sistema qué app está realmente en pantalla:
 *      · YouTube  -> se muestra el ejercicio y el timer espera a que se resuelva;
 *                    al resolverse, se vuelve a contar el tiempo completo.
 *      · otra app -> el timer se detiene (se reactiva solo al volver a una app vigilada).
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

        // Tiempo que falta para el próximo ejercicio. Baja de a 1 s, y con "contar solo mientras
        // se reproduce" solo baja si YouTube está en pantalla Y hay audio sonando.
        private var restanteMs = 0L
        private var contando = true

        private val tick = object : Runnable {
            override fun run() {
                alTick()
            }
        }

        private fun reproduciendo(ctx: Context): Boolean {
            val (pantalla, _) = Deteccion.clasificar(ctx)
            if (pantalla != Pantalla.VIGILADA) return false
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            return am.isMusicActive
        }

        private fun alTick() {
            val ctx = appContext ?: return
            if (!running || esperandoCierre) return
            val cuenta = if (SettingsStore.getSoloReproduciendo(ctx)) reproduciendo(ctx) else true
            if (cuenta != contando) {
                contando = cuenta
                TranqueraLog.d(if (cuenta) "TIMER cuenta reanudada" else "TIMER cuenta en pausa (no hay audio sonando)")
            }
            if (cuenta) restanteMs -= 1000
            proximaEsperada = SystemClock.elapsedRealtime() + restanteMs
            if (restanteMs <= 0) alSonar() else handler.postDelayed(tick, 1000)
        }

        private fun alSonar() {
            val ctx = appContext ?: return
            if (!running) return
            if (!SettingsStore.getActivo(ctx)) {
                running = false
                esperandoCierre = false
                TranqueraLog.d("TIMER detenido: la tranquera está apagada en los ajustes")
                return
            }

            val retraso = SystemClock.elapsedRealtime() - proximaEsperada
            val (pantalla, paquete) = Deteccion.clasificar(ctx)
            TranqueraLog.d(
                "TIMER sonó. en_pantalla=${Deteccion.etiqueta(ctx, paquete)} -> $pantalla retraso_vs_esperado=${retraso}ms"
            )

            when (pantalla) {
                Pantalla.VIGILADA -> {
                    TranqueraOverlayManager.show(ctx)
                    // No se programa otro ciclo: se espera a que se resuelva el ejercicio
                    esperandoCierre = true
                    TranqueraLog.d("TIMER en espera hasta que se resuelva el ejercicio")
                }
                Pantalla.OTRA_APP -> {
                    running = false
                    TranqueraLog.d("TIMER detenido: el usuario está en otra app (${Deteccion.etiqueta(ctx, paquete)}). Se reactiva al volver a una app vigilada")
                }
                Pantalla.INCIERTO -> {
                    TranqueraLog.d("TIMER ciclo salteado: pantalla incierta, se sigue contando")
                    programar(ctx)
                }
            }
        }

        private fun programar(ctx: Context) {
            val intervalo = SettingsStore.getIntervalMs(ctx)
            esperandoCierre = false
            restanteMs = intervalo
            contando = true
            proximaEsperada = SystemClock.elapsedRealtime() + intervalo
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, 1000)
            TranqueraLog.d("TIMER programado: próximo en ${intervalo / 1000}s")
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
            TranqueraLog.d("TIMER reiniciado tras resolver el ejercicio")
        }

        /** Para el timer por completo (interruptor apagado). */
        fun detener() {
            handler.removeCallbacks(tick)
            running = false
            esperandoCierre = false
            TranqueraLog.d("TIMER detenido por el interruptor")
        }

        /** Se llama al guardar un tiempo nuevo: si hay un ciclo en curso, se reprograma ya. */
        fun reprogramar(context: Context) {
            if (!running || esperandoCierre) return
            handler.removeCallbacks(tick)
            programar(context.applicationContext)
            TranqueraLog.d("TIMER reprogramado por cambio de ajustes")
        }

        /** Para el panel de estado: "corriendo, próximo en 12 s", "esperando ejercicio" o "detenido". */
        fun descripcionEstado(): String = when {
            !running -> "detenido (se activa al abrir una app vigilada)"
            esperandoCierre -> "esperando que se resuelva el ejercicio"
            else -> {
                val faltan = (restanteMs / 1000).coerceAtLeast(0)
                val pausa = if (contando) "" else " (en pausa: no hay audio sonando)"
                "corriendo, próximo ejercicio en ${faltan}s$pausa"
            }
        }
    }
}
