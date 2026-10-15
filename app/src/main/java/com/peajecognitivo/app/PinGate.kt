package com.peajecognitivo.app

import android.app.Activity
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** PIN de padres: protege la pantalla de ajustes y el registro. */
object PinGate {
    private const val VALIDEZ_MS = 5 * 60 * 1000L
    private var desbloqueadoHasta = 0L

    private fun campo(activity: Activity, pista: String): EditText = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = pista
        setPadding(48, 32, 48, 32)
    }

    /** Si hay PIN y no se desbloqueó hace poco, lo pide. Si se cancela, cierra la pantalla. */
    fun exigir(activity: Activity) {
        val pin = SettingsStore.getPin(activity)
        if (pin.isEmpty() || SystemClock.elapsedRealtime() < desbloqueadoHasta) return

        val contenido = activity.findViewById<View>(android.R.id.content)
        contenido.visibility = View.INVISIBLE
        val et = campo(activity, "PIN")
        val dialogo = AlertDialog.Builder(activity)
            .setTitle("🔒 PIN de padres")
            .setView(et)
            .setCancelable(false)
            .setPositiveButton("Entrar", null)
            .setNegativeButton("Salir") { _, _ -> activity.finish() }
            .create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (et.text.toString() == pin) {
                    desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
                    contenido.visibility = View.VISIBLE
                    dialogo.dismiss()
                } else {
                    et.text.clear()
                    et.error = "PIN incorrecto"
                }
            }
        }
        dialogo.show()
    }

    /** Diálogo para poner, cambiar o quitar el PIN (vacío = sin PIN). */
    fun configurar(activity: Activity) {
        val et = campo(activity, "4 a 6 números (vacío = sin PIN)")
        AlertDialog.Builder(activity)
            .setTitle("🔒 PIN de padres")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = et.text.toString().trim()
                if (nuevo.isEmpty()) {
                    SettingsStore.setPin(activity, "")
                    Toast.makeText(activity, "PIN quitado", Toast.LENGTH_SHORT).show()
                } else if (nuevo.length < 4 || nuevo.length > 6) {
                    Toast.makeText(activity, "El PIN debe tener de 4 a 6 números", Toast.LENGTH_SHORT).show()
                } else {
                    SettingsStore.setPin(activity, nuevo)
                    desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
                    Toast.makeText(activity, "PIN guardado ✓", Toast.LENGTH_SHORT).show()
                }
                PeajeLog.d("AJUSTES PIN ${if (SettingsStore.getPin(activity).isEmpty()) "sin PIN" else "con PIN"}")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
