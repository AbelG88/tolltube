package com.peajecognitivo.app

import android.app.Activity
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * PIN de padres: protege los ajustes, el registro y el "Apagar" de la notificación.
 * El PIN se guarda con hash (ver [PinStore]); acá solo está la parte de pantallas.
 */
object PinGate {
    private const val VALIDEZ_MS = 60 * 1000L
    private var desbloqueadoHasta = 0L
    private var dialogoAbierto = false

    private fun campo(activity: Activity, pista: String): EditText = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = pista
        setPadding(48, 32, 48, 32)
    }

    /** Vuelve a pedir el PIN la próxima vez (se llama al salir de la app o apagar la pantalla). */
    fun bloquear() {
        desbloqueadoHasta = 0L
    }

    /** Diálogo que pide el PIN actual y lo comprueba (con bloqueo tras varios fallos). */
    private fun pedirPin(
        activity: Activity, titulo: String, textoOk: String, textoCancelar: String,
        alCorrecto: () -> Unit, alCancelar: () -> Unit
    ) {
        val et = campo(activity, "PIN")
        val dialogo = AlertDialog.Builder(activity)
            .setTitle("🔒 $titulo")
            .setView(et)
            .setCancelable(false)
            .setPositiveButton(textoOk, null)
            .setNegativeButton(textoCancelar) { _, _ -> alCancelar() }
            .create()
        dialogoAbierto = true
        dialogo.setOnDismissListener { dialogoAbierto = false }
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ingresado = et.text.toString()
                if (ingresado.isEmpty()) return@setOnClickListener
                when (val r = PinStore.verificar(activity, ingresado)) {
                    is ResultadoPin.Correcto -> {
                        dialogo.dismiss()
                        alCorrecto()
                    }
                    is ResultadoPin.Incorrecto -> {
                        et.text.clear()
                        et.error = "PIN incorrecto (${r.intentosRestantes} intentos antes del bloqueo)"
                    }
                    is ResultadoPin.Bloqueado -> {
                        et.text.clear()
                        et.error = "Demasiados intentos. Esperá ${r.segundos} s"
                    }
                }
            }
        }
        dialogo.show()
    }

    /** Si hay PIN y no se desbloqueó hace poco, lo pide. Si se cancela, cierra la pantalla. */
    fun exigir(activity: Activity) {
        if (!PinStore.tienePin(activity) || SystemClock.elapsedRealtime() < desbloqueadoHasta) return
        if (dialogoAbierto) return

        val contenido = activity.findViewById<View>(android.R.id.content)
        contenido.visibility = View.INVISIBLE
        pedirPin(
            activity, "PIN de padres", "Entrar", "Salir",
            alCorrecto = {
                desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
                contenido.visibility = View.VISIBLE
            },
            alCancelar = { activity.finish() }
        )
    }

    /** Sin PIN no hay protección: la primera vez hay que crear uno (no se puede saltear). */
    fun exigirCrear(activity: Activity) {
        if (PinStore.tienePin(activity) || dialogoAbierto) return
        dialogoNuevoPin(activity, obligatorio = true)
    }

    /**
     * Pide el PIN para una acción sensible que se dispara desde fuera de la app (p. ej. la notificación).
     * Sin PIN configurado no se permite la acción.
     */
    fun pedirParaAccion(activity: Activity, titulo: String, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
        if (!PinStore.tienePin(activity)) {
            Toast.makeText(
                activity, "Primero poné un PIN de padres en la app para poder apagar desde acá", Toast.LENGTH_LONG
            ).show()
            alCancelar()
            return
        }
        pedirPin(activity, titulo, "Confirmar", "Cancelar", alConfirmar, alCancelar)
    }

    /** Cambiar el PIN: si ya hay uno, primero hay que ingresar el actual. */
    fun configurar(activity: Activity) {
        if (PinStore.tienePin(activity)) {
            pedirPin(
                activity, "PIN actual", "Continuar", "Cancelar",
                alCorrecto = { dialogoNuevoPin(activity, obligatorio = false) },
                alCancelar = { }
            )
        } else {
            dialogoNuevoPin(activity, obligatorio = false)
        }
    }

    private fun dialogoNuevoPin(activity: Activity, obligatorio: Boolean) {
        val nuevo = campo(activity, "PIN nuevo (4 a 6 números)")
        val repetir = campo(activity, "Repetir el PIN")
        val contenido = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(nuevo)
            addView(repetir)
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle(if (obligatorio) "🔒 Poné un PIN de padres" else "🔒 PIN de padres")
            .setView(contenido)
            .setCancelable(false)
            .setPositiveButton("Guardar", null)
        if (!obligatorio) builder.setNegativeButton("Cancelar", null)
        val dialogo = builder.create()
        dialogoAbierto = true
        dialogo.setOnDismissListener { dialogoAbierto = false }
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val a = nuevo.text.toString().trim()
                val b = repetir.text.toString().trim()
                when {
                    a.length < 4 || a.length > 6 -> nuevo.error = "Tiene que tener de 4 a 6 números"
                    a != b -> repetir.error = "No coincide"
                    else -> {
                        PinStore.guardar(activity, a)
                        desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
                        Toast.makeText(activity, "PIN guardado ✓", Toast.LENGTH_SHORT).show()
                        PeajeLog.d("AJUSTES PIN guardado")
                        dialogo.dismiss()
                    }
                }
            }
        }
        dialogo.show()
    }
}
