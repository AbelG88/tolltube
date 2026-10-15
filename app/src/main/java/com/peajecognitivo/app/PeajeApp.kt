package com.peajecognitivo.app

import android.app.Application

/**
 * Corre al arrancar el proceso (también cuando Android relanza el servicio de accesibilidad).
 * Prepara el registro y deja anotado cualquier crash antes de que el proceso muera.
 */
class PeajeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PeajeLog.attach(this)
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
