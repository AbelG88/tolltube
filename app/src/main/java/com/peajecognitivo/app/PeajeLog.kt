package com.peajecognitivo.app

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Logger de diagnóstico. Hace dos cosas:
 *  1. Escribe en logcat con el tag "Peaje" (adb logcat -s Peaje:D)
 *  2. Guarda las líneas en un archivo del celular, que sobrevive aunque el proceso muera.
 *     Se puede ver, copiar y compartir desde la app ("Ver registro").
 *
 * Cada línea lleva la hora y [+Nms]: tiempo desde que arrancó ESTE proceso.
 * Si el contador vuelve a valores chicos, el proceso se reinició.
 */
object PeajeLog {
    const val TAG = "Peaje"
    private const val ARCHIVO = "peaje-log.txt"
    private const val MAX_BYTES = 150_000L

    private val t0 = SystemClock.elapsedRealtime()
    private val formato = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val escritor = Executors.newSingleThreadExecutor()

    @Volatile
    private var archivo: File? = null

    /** Se llama una vez al arrancar el proceso (desde PeajeApp). */
    fun attach(context: Context) {
        if (!BuildConfig.LOG_DIAGNOSTICO) {
            // Versión para publicar: no se registra nada y se borra cualquier registro viejo
            try { File(context.applicationContext.filesDir, ARCHIVO).delete() } catch (e: Exception) { }
            return
        }
        if (archivo == null) {
            archivo = File(context.applicationContext.filesDir, ARCHIVO)
            d("PROCESO iniciado (pid=${android.os.Process.myPid()})")
        }
    }

    fun d(msg: String) {
        if (!BuildConfig.LOG_DIAGNOSTICO) return
        val t = SystemClock.elapsedRealtime() - t0
        Log.d(TAG, "[+${t}ms] $msg")
        val f = archivo ?: return
        val linea = "${hora()} [+${t}ms] $msg"
        escritor.execute {
            try {
                escribir(f, linea)
            } catch (e: Exception) {
                // sin registro no hay nada más que hacer
            }
        }
    }

    private fun hora(): String = synchronized(formato) { formato.format(Date()) }

    private fun escribir(f: File, linea: String) {
        if (f.exists() && f.length() > MAX_BYTES) {
            val lineas = f.readLines()
            f.writeText(lineas.takeLast(lineas.size / 2).joinToString("\n") + "\n")
        }
        f.appendText(linea + "\n")
    }

    /** Espera a que se terminen de escribir las líneas pendientes (para el momento de un crash). */
    fun flush() {
        try {
            escritor.submit(Runnable { }).get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // ignorar
        }
    }

    /** Últimas [ultimas] líneas del registro. */
    fun leer(context: Context, ultimas: Int = 300): String {
        val f = archivo ?: File(context.applicationContext.filesDir, ARCHIVO)
        return try {
            escritor.submit(Callable {
                if (!f.exists()) "(registro vacío)"
                else f.readLines().takeLast(ultimas).joinToString("\n").ifBlank { "(registro vacío)" }
            }).get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            "(no se pudo leer el registro: ${e.javaClass.simpleName})"
        }
    }

    fun borrar(context: Context) {
        val f = archivo ?: File(context.applicationContext.filesDir, ARCHIVO)
        try {
            escritor.submit(Callable { f.delete() }).get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // ignorar
        }
        d("REGISTRO borrado por el usuario")
    }
}
