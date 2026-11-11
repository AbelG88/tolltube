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
    private const val VALIDEZ_MS = 60 * 1000L
    private var desbloqueadoHasta = 0L
    private var dialogoAbierto = false
    private var fallos = 0
    private var bloqueadoHasta = 0L

    private fun campo(activity: Activity, pista: String): EditText = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = pista
        setPadding(48, 32, 48, 32)
    }

    /** Si hay PIN y no se desbloqueó hace poco, lo pide. Si se cancela, cierra la pantalla. */
    fun exigir(activity: Activity) {
        val pin = SettingsStore.getPin(activity)
        if (pin.isEmpty() || SystemClock.elapsedRealtime() < desbloqueadoHasta) return
        if (dialogoAbierto) return
        dialogoAbierto = true

        val contenido = activity.findViewById<View>(android.R.id.content)
        contenido.visibility = View.INVISIBLE
        val et = campo(activity, "PIN")
        val dialogo = AlertDialog.Builder(activity)
            .setTitle("🔒 PIN de padres")
            .setView(et)
            .setCancelable(false)
            .setPositiveButton("Entrar", null)
            .setNegativeButton("Salir") { _, _ ->
                dialogoAbierto = false
                activity.finish()
            }
            .create()
        dialogo.setOnDismissListener { dialogoAbierto = false }
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (et.text.toString() == pin) {
                    desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
                    contenido.visibility = View.VISIBLE
                    dialogoAbierto = false
                    dialogo.dismiss()
                } else {
                    et.text.clear()
                    et.error = "PIN incorrecto"
                }
            }
        }
        dialogo.show()
    }

    /**
     * Pide el PIN para una acción sensible que se dispara desde fuera de la app (p. ej. la notificación).
     * Sin PIN configurado no se permite la acción. Tras 5 fallos se bloquea el ingreso un rato,
     * cada vez más largo.
     */
    fun pedirParaAccion(activity: Activity, titulo: String, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
        val pin = SettingsStore.getPin(activity)
        if (pin.isEmpty()) {
            Toast.makeText(
                activity, "Primero poné un PIN de padres en la app para poder apagar desde acá", Toast.LENGTH_LONG
            ).show()
            alCancelar()
            return
        }
        val et = campo(activity, "PIN")
        val dialogo = AlertDialog.Builder(activity)
            .setTitle("🔒 $titulo")
            .setView(et)
            .setCancelable(false)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar") { _, _ -> alCancelar() }
            .create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ahora = SystemClock.elapsedRealtime()
                if (ahora < bloqueadoHasta) {
                    et.error = "Demasiados intentos. Esperá ${(bloqueadoHasta - ahora) / 1000 + 1} s"
                    return@setOnClickListener
                }
                if (et.text.toString() == pin) {
                    fallos = 0
                    dialogo.dismiss()
                    alConfirmar()
                } else {
                    fallos++
                    et.text.clear()
                    if (fallos >= 5) {
                        bloqueadoHasta = ahora + 30_000L * (fallos - 4)
                        et.error = "Demasiados intentos. Esperá ${(bloqueadoHasta - ahora) / 1000} s"
                    } else {
                        et.error = "PIN incorrecto"
                    }
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
                    desbloqueadoHasta = 0L // para que se pida la próxima vez que se entre
                    Toast.makeText(activity, "PIN guardado ✓", Toast.LENGTH_SHORT).show()
                }
                PeajeLog.d("AJUSTES PIN ${if (SettingsStore.getPin(activity).isEmpty()) "sin PIN" else "con PIN"}")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
