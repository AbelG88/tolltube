package com.tranquera.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Destino de la acción "Abrir" (la tranquera) de la notificación. Sin pantalla propia: pide el PIN de padres
 * y, si es correcto, abre la tranquera y detiene el servicio (la notificación desaparece).
 */
class ApagarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PinGate.pedirParaAccion(
            this,
            titulo = "Abrir la tranquera",
            alConfirmar = {
                SettingsStore.setActivo(this, false)
                ExerciseTimerManager.detener()
                TranqueraLog.d("NOTIF tranquera abierta desde la notificación (con PIN)")
                TranqueraMonitorService.detener(this)
                Toast.makeText(this, "Tranquera abierta. Se cierra de nuevo desde la app.", Toast.LENGTH_LONG).show()
                finish()
            },
            alCancelar = { finish() }
        )
    }
}
