package com.peajecognitivo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Servicio en primer plano para el modo "Uso de apps": cada pocos segundos mira qué app está
 * en pantalla y arranca el timer cuando aparece YouTube. Muestra una notificación fija.
 */
class PeajeMonitorService : Service() {

    companion object {
        @Volatile
        var vivo = false
            private set

        private const val CANAL = "peaje_monitor"
        private const val ID_NOTIF = 1001
        private const val CADA_MS = 1500L

        fun iniciar(context: Context) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, PeajeMonitorService::class.java)
            )
        }

        fun detener(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, PeajeMonitorService::class.java)
            )
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val ciclo = object : Runnable {
        override fun run() {
            if (!vivo) return
            UsoApps.actualizar(this@PeajeMonitorService)
            val (pantalla, _) = UsoApps.clasificar(this@PeajeMonitorService)
            if (pantalla == Pantalla.YOUTUBE) {
                ExerciseTimerManager.startTimer(this@PeajeMonitorService)
            }
            handler.postDelayed(this, CADA_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Peaje Cognitivo activo", NotificationManager.IMPORTANCE_LOW)
        )
        val notificacion = Notification.Builder(this, CANAL)
            .setContentTitle("Peaje Cognitivo")
            .setContentText("Cuidando el tiempo de pantalla")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_NOTIF, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_NOTIF, notificacion)
        }
        if (!vivo) PeajeLog.d("MONITOR servicio iniciado")
        vivo = true
        handler.removeCallbacks(ciclo)
        handler.post(ciclo)
        return START_STICKY
    }

    override fun onDestroy() {
        vivo = false
        handler.removeCallbacks(ciclo)
        PeajeLog.d("MONITOR servicio detenido")
        super.onDestroy()
    }
}
