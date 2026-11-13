package com.tranquera.app

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.Toast

/**
 * PIN de padres: protege los ajustes, el registro y el "Abrir" de la notificación.
 * El PIN se guarda con hash (ver [PinStore]); acá solo está la parte de pantallas.
 */
object PinGate {
    private const val VALIDEZ_MS = 60 * 1000L
    private var desbloqueadoHasta = 0L

    /** Tras crear el PIN en el asistente, no se vuelve a pedir enseguida. */
    fun desbloquearAhora() {
        desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
    }

    private var dialogoAbierto = false

    /** Vuelve a pedir el PIN la próxima vez (se llama al salir de la app o apagar la pantalla). */
    fun bloquear() {
        desbloqueadoHasta = 0L
    }

    /** Pantalla de PIN que pide el PIN actual y lo comprueba (con bloqueo tras varios fallos). */
    private fun pedirPin(
        activity: Activity, titulo: String, textoOk: String, textoCancelar: String,
        alCorrecto: () -> Unit, alCancelar: () -> Unit
    ) {
        val pad = PinPad(
            activity, titulo, "Ingresá el PIN de padres para continuar.", dosCampos = false,
            textoBoton = textoOk, textoCancelar = textoCancelar,
            alEnviar = { p, ingresado, _ ->
                when (val r = PinStore.verificar(activity, ingresado)) {
                    is ResultadoPin.Correcto -> {
                        p.cerrar()
                        alCorrecto()
                    }
                    is ResultadoPin.Incorrecto ->
                        p.error("PIN incorrecto. Te quedan ${r.intentosRestantes} intentos antes del bloqueo.")
                    is ResultadoPin.Bloqueado ->
                        p.error("Demasiados intentos. Esperá ${r.segundos} s.")
                }
            },
            alCancelar = { p ->
                p.cerrar()
                alCancelar()
            }
        )
        dialogoAbierto = true
        pad.dialogo.setOnDismissListener { dialogoAbierto = false }
        pad.mostrar()
    }

    /** true si no hay PIN o el panel ya está desbloqueado (no hay un pedido de PIN en pantalla). */
    fun libre(context: android.content.Context): Boolean =
        !PinStore.tienePin(context) || SystemClock.elapsedRealtime() < desbloqueadoHasta

    /** Si hay PIN y no se desbloqueó hace poco, lo pide. Si se cancela, cierra la pantalla. */
    fun exigir(activity: Activity) {
        if (!PinStore.tienePin(activity) || SystemClock.elapsedRealtime() < desbloqueadoHasta) return
        if (dialogoAbierto) return

        val contenido = activity.findViewById<View>(android.R.id.content)
        contenido.visibility = View.INVISIBLE
        pedirPin(
            activity, "PIN de padres", "Abrir", "Salir",
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
                activity, "Primero poné un PIN de padres en la app para poder abrir la tranquera desde acá", Toast.LENGTH_LONG
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
        elegirNuevo(activity, obligatorio) { pin ->
            PinStore.guardar(activity, pin)
            desbloqueadoHasta = SystemClock.elapsedRealtime() + VALIDEZ_MS
            Toast.makeText(activity, "PIN guardado ✓", Toast.LENGTH_SHORT).show()
            TranqueraLog.d("AJUSTES PIN guardado")
        }
    }

    /**
     * Pide un PIN nuevo (PIN y repetirlo) sin guardarlo: lo entrega a [alElegir].
     * Si [obligatorio] es false, se puede cancelar.
     */
    fun elegirNuevo(activity: Activity, obligatorio: Boolean, alElegir: (String) -> Unit) {
        val pad = PinPad(
            activity,
            if (obligatorio) "Poné un PIN de padres" else "PIN de padres",
            "Es para entrar a los ajustes. Elegí al menos ${PinPad.MIN_LARGO} números que el peque no sepa.",
            dosCampos = true,
            textoBoton = "Guardar",
            textoCancelar = if (obligatorio) null else "Cancelar",
            alEnviar = { p, pin, repetido ->
                when {
                    pin.length < PinPad.MIN_LARGO ->
                        p.error("El PIN tiene que tener al menos ${PinPad.MIN_LARGO} números.")
                    pin != repetido -> p.error("Los dos PIN no coinciden.")
                    else -> {
                        p.cerrar()
                        alElegir(pin)
                    }
                }
            },
            alCancelar = { p -> p.cerrar() }
        )
        dialogoAbierto = true
        pad.dialogo.setOnDismissListener { dialogoAbierto = false }
        pad.mostrar()
    }
}
