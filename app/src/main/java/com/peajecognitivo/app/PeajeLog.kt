package com.peajecognitivo.app

import android.os.SystemClock
import android.util.Log

/**
 * Logger de diagnóstico. Todo sale con el tag "Peaje" para filtrar con:
 *   adb logcat -s Peaje:D
 * Cada línea lleva el tiempo desde el arranque del proceso (ms) para poder
 * medir cuánto tarda en sonar la alarma y cuándo se cancela el timer.
 */
object PeajeLog {
    const val TAG = "Peaje"
    private val t0 = SystemClock.elapsedRealtime()

    fun d(msg: String) {
        val t = SystemClock.elapsedRealtime() - t0
        Log.d(TAG, "[+${t}ms] $msg")
    }
}
