package com.peajecognitivo.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Corre al arrancar el proceso (también cuando Android relanza el servicio de accesibilidad).
 * Prepara el registro y deja anotado cualquier crash antes de que el proceso muera.
 */
class PeajeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PeajeLog.attach(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var iniciadas = 0
            private val handler = Handler(Looper.getMainLooper())
            private val bloquear = Runnable { PinGate.bloquear() }
            override fun onActivityStarted(a: Activity) {
                iniciadas++
                handler.removeCallbacks(bloquear)
            }
            override fun onActivityStopped(a: Activity) {
                iniciadas = (iniciadas - 1).coerceAtLeast(0)
                if (iniciadas == 0) handler.postDelayed(bloquear, 3000) // toda la app salió de pantalla
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        PeajeLog.d("PROCESO versión=$version")

        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            PeajeLog.d(
                "CRASH en hilo '${hilo.name}': ${error.javaClass.name}: ${error.message}\n" +
                    error.stackTraceToString().take(1200)
            )
            PeajeLog.flush()
            anterior?.uncaughtException(hilo, error)
        }
    }
}
