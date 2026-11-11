package com.peajecognitivo.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Destino de la acción "Apagar" de la notificación. Sin pantalla propia: pide el PIN de padres
 * y, si es correcto, apaga el peaje y detiene el servicio (la notificación desaparece).
 */
class ApagarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PinGate.pedirParaAccion(
            this,
            titulo = "Apagar el peaje",
            alConfirmar = {
                SettingsStore.setActivo(this, false)
                ExerciseTimerManager.detener()
                PeajeLog.d("NOTIF peaje apagado desde la notificación (con PIN)")
                PeajeMonitorService.detener(this)
                Toast.makeText(this, "Peaje apagado. Se vuelve a prender desde la app.", Toast.LENGTH_LONG).show()
                finish()
            },
            alCancelar = { finish() }
        )
    }
}
