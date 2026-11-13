package com.tranquera.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Servicio en primer plano para el modo "Uso de apps": cada pocos segundos mira qué app está
 * en pantalla y arranca el timer cuando aparece una app vigilada. Muestra una notificación fija.
 */
class TranqueraMonitorService : Service() {

    companion object {
        @Volatile
        var vivo = false
            private set

        private const val CANAL = "tranquera_monitor"
        private const val ID_NOTIF = 1001
        private const val CADA_MS = 1500L

        fun iniciar(context: Context) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, TranqueraMonitorService::class.java)
            )
        }

        fun detener(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, TranqueraMonitorService::class.java)
            )
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val ciclo = object : Runnable {
        override fun run() {
            if (!vivo) return
            UsoApps.actualizar(this@TranqueraMonitorService)
            val (pantalla, _) = UsoApps.clasificar(this@TranqueraMonitorService)
            if (pantalla == Pantalla.VIGILADA) {
                ExerciseTimerManager.startTimer(this@TranqueraMonitorService)
            }
            handler.postDelayed(this, CADA_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Tranquera cerrada", NotificationManager.IMPORTANCE_LOW)
        )
        // Tocar la notificación abre la app (que pide el PIN si hay uno)
        val abrirApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // "Abrir" pide el PIN de padres; sin PIN configurado no abre nada
        val apagar = PendingIntent.getActivity(
            this, 1,
            Intent(this, ApagarActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = Notification.Builder(this, CANAL)
            .setContentTitle("Tranquera")
            .setContentText("Tranquera cerrada: cuidando el tiempo de pantalla")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(abrirApp)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_lock_power_off), "Abrir (PIN)", apagar
                ).build()
            )
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_NOTIF, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_NOTIF, notificacion)
        }
        if (!vivo) TranqueraLog.d("MONITOR servicio iniciado")
        vivo = true
        handler.removeCallbacks(ciclo)
        handler.post(ciclo)
        return START_STICKY
    }

    override fun onDestroy() {
        vivo = false
        handler.removeCallbacks(ciclo)
        TranqueraLog.d("MONITOR servicio detenido")
        super.onDestroy()
    }
}
