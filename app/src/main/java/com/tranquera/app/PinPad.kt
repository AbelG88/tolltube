package com.tranquera.app

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.animation.CycleInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * Pantalla de PIN: candado, puntitos que se llenan y teclado numérico redondo.
 * Se confirma sola al completar [LARGO] números (mismo estilo que la app Nappy).
 */
class PinPad(
    private val activity: Activity,
    titulo: String,
    subtitulo: String,
    textoCancelar: String?,                 // null = no se puede cancelar
    private val alCompletar: (PinPad, String) -> Unit,
    private val alCancelar: (PinPad) -> Unit
) {
    companion object {
        const val LARGO = 4
    }

    val dialogo = Dialog(activity)
    private val ingresado = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val puntos = ArrayList<View>()
    private val tvTitulo: TextView
    private val tvSubtitulo: TextView
    private val tvMensaje: TextView
    private val filaPuntos: LinearLayout
    private var enError = false

    private val azul = activity.getColor(R.color.accent)
    private val azulSuave = activity.getColor(R.color.accent_container)
    private val textoPrincipal = activity.getColor(R.color.text_primary)
    private val textoSecundario = activity.getColor(R.color.text_secondary)
    private val rojo = Color.parseColor("#EF4444")
    private val gris = Color.parseColor("#CBD5E1")
    private val negrita: Typeface? = ResourcesCompat.getFont(activity, R.font.quicksand_bold)
    private val media: Typeface? = ResourcesCompat.getFont(activity, R.font.quicksand_medium)

    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    init {
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogo.setCancelable(false)

        val tarjeta = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(32).toFloat()
            }
        }

        // Candado en un círculo celeste
        tarjeta.addView(TextView(activity).apply {
            text = "🔒"
            textSize = 28f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(azulSuave)
            }
        }, LinearLayout.LayoutParams(dp(64), dp(64)))

        tvTitulo = TextView(activity).apply {
            textSize = 22f
            typeface = negrita
            setTextColor(textoPrincipal)
            gravity = Gravity.CENTER
        }
        tarjeta.addView(tvTitulo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        tvSubtitulo = TextView(activity).apply {
            textSize = 14f
            typeface = media
            setTextColor(textoSecundario)
            gravity = Gravity.CENTER
        }
        tarjeta.addView(tvSubtitulo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        // Puntitos
        filaPuntos = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(LARGO) {
            val p = View(activity)
            puntos.add(p)
            filaPuntos.addView(p, LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                marginStart = dp(9); marginEnd = dp(9)
            })
        }
        tarjeta.addView(filaPuntos, LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = dp(22); gravity = Gravity.CENTER_HORIZONTAL
        })

        // Mensaje de error (reserva su lugar para que el teclado no salte)
        tvMensaje = TextView(activity).apply {
            textSize = 13f
            typeface = media
            setTextColor(rojo)
            gravity = Gravity.CENTER
            minLines = 2
        }
        tarjeta.addView(tvMensaje, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        // Teclado 3 x 4
        val filas = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("cancelar", "0", "borrar")
        )
        for (fila in filas) {
            val f = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            for (t in fila) f.addView(tecla(t, textoCancelar), LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                marginStart = dp(8); marginEnd = dp(8); topMargin = dp(6); bottomMargin = dp(6)
            })
            tarjeta.addView(f)
        }

        val envoltorio = FrameLayout(activity).apply {
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(tarjeta, FrameLayout.LayoutParams(-1, -2))
        }
        dialogo.setContentView(ScrollView(activity).apply { addView(envoltorio) })
        dialogo.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(-1, -2)
        }

        ponerTextos(titulo, subtitulo)
        pintarPuntos()
    }

    /** Una tecla: dígito, "borrar" o "cancelar". */
    private fun tecla(clave: String, textoCancelar: String?): View {
        if (clave == "cancelar" && textoCancelar == null) return View(activity)
        val v = TextView(activity).apply {
            gravity = Gravity.CENTER
            typeface = negrita
            setTextColor(textoPrincipal)
            when (clave) {
                "cancelar" -> { text = textoCancelar; textSize = 14f; setTextColor(textoSecundario) }
                "borrar" -> { text = "⌫"; textSize = 24f }
                else -> { text = clave; textSize = 28f }
            }
        }
        if (clave != "cancelar") {
            val fondo = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F1F5F9"))
            }
            v.background = RippleDrawable(ColorStateList.valueOf(azulSuave), fondo, null)
        }
        v.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            when (clave) {
                "cancelar" -> alCancelar(this)
                "borrar" -> borrar()
                else -> agregar(clave)
            }
        }
        return v
    }

    private fun agregar(d: String) {
        if (enError || ingresado.length >= LARGO) return
        tvMensaje.text = ""
        ingresado.append(d)
        pintarPuntos()
        if (ingresado.length == LARGO) {
            val pin = ingresado.toString()
            // Deja ver el último puntito lleno antes de comprobar
            handler.postDelayed({ if (dialogo.isShowing) alCompletar(this, pin) }, 150)
        }
    }

    private fun borrar() {
        if (enError || ingresado.isEmpty() || ingresado.length >= LARGO) return
        ingresado.deleteCharAt(ingresado.length - 1)
        pintarPuntos()
    }

    private fun pintarPuntos() {
        puntos.forEachIndexed { i, p ->
            p.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                when {
                    enError -> setColor(rojo)
                    i < ingresado.length -> setColor(azul)
                    else -> { setColor(Color.WHITE); setStroke(dp(2), gris) }
                }
            }
        }
    }

    fun ponerTextos(titulo: String, subtitulo: String) {
        tvTitulo.text = titulo
        tvSubtitulo.text = subtitulo
    }

    /** Borra lo escrito y deja los puntitos vacíos. */
    fun limpiar() {
        ingresado.setLength(0)
        enError = false
        pintarPuntos()
    }

    /** Puntitos en rojo, sacudida y mensaje; después se vacía solo. */
    fun error(mensaje: String) {
        enError = true
        tvMensaje.text = mensaje
        pintarPuntos()
        ObjectAnimator.ofFloat(filaPuntos, View.TRANSLATION_X, 0f, 18f).apply {
            duration = 350
            interpolator = CycleInterpolator(3f)
            start()
        }
        handler.postDelayed({ limpiar() }, 450)
    }

    fun mostrar() = dialogo.show()

    fun cerrar() {
        handler.removeCallbacksAndMessages(null)
        if (dialogo.isShowing) dialogo.dismiss()
    }
}
