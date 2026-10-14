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

        // Solo diagnóstico: cuándo debía sonar la próxima alarma (elapsedRealtime)
        var proximaAlarmaEsperada = 0L
            private set

        fun startTimer(context: Context) {
            // Si ya hay un timer activo, no crear otro
            if (pendingIntent != null) {
                PeajeLog.d("TIMER startTimer ignorado: ya había un timer activo")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ExerciseAlarmReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            proximaAlarmaEsperada = SystemClock.elapsedRealtime() + INTERVAL_MS
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                proximaAlarmaEsperada,
                INTERVAL_MS,
                pendingIntent!!
            )
            PeajeLog.d("TIMER programado: cada ${INTERVAL_MS / 1000}s (alarma inexacta)")
        }

        fun pauseTimer(context: Context) {
            val pi = pendingIntent
            if (pi == null) {
                PeajeLog.d("TIMER pauseTimer: no había timer en memoria (la alarma real puede seguir viva)")
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pi)
            pendingIntent = null
            PeajeLog.d("TIMER cancelado")
        }
    }
}

class ExerciseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ahora = SystemClock.elapsedRealtime()
        val esperada = ExerciseTimerManager.proximaAlarmaEsperada
        val retraso = if (esperada > 0) ahora - esperada else -1
        PeajeLog.d(
            "ALARMA sonó. isYouTubeActive=${PeajeAccessibilityService.isYouTubeActive} " +
                "retraso_vs_esperada=${retraso}ms"
        )
        if (PeajeAccessibilityService.isYouTubeActive) {
            PeajeOverlayManager.show(context)
        } else {
            PeajeLog.d("ALARMA ignorada: no se cree que YouTube esté en primer plano")
        }
    }
}
