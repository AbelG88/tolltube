package com.peajecognitivo.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class ExerciseTimerManager {
    companion object {
        // CAMBIAR ESTO PARA PROBAR: 60000 = 1 minuto, 600000 = 10 minutos
        private const val INTERVAL_MS = 60_000L // 1 minuto para pruebas. Cambiar a 10*60*1000 para producción
        private const val ALARM_ACTION = "com.peajecognitivo.TRIGGER_EXERCISE"
        private var pendingIntent: PendingIntent? = null

        fun startTimer(context: Context) {
            // Si ya hay un timer activo, no crear otro
            if (pendingIntent != null) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ExerciseAlarmReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_MS,
                INTERVAL_MS,
                pendingIntent!!
            )
        }

        fun pauseTimer(context: Context) {
            pendingIntent?.let {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(it)
                pendingIntent = null
            }
        }
    }
}

class ExerciseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (PeajeAccessibilityService.isYouTubeActive) {
            PeajeOverlayManager.show(context)
        }
    }
}