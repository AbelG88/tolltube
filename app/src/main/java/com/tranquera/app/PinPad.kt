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
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.CycleInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged

/**
 * Pantalla de PIN con el mismo look que la pantalla de bloqueo de Nappy: fondo celeste, tarjeta
 * blanca muy redondeada, candado blanco sobre un cuadrado azul, campo con ojito para ver/ocultar
 * y botón azul grande. El PIN son números (mínimo [MIN_LARGO]).
 *
 * Con [dosCampos] pide el PIN y su repetición (para crear o cambiar el PIN).
 */
class PinPad(
    private val activity: Activity,
    titulo: String,
    texto: String,
    private val dosCampos: Boolean,
    textoBoton: String,
    textoCancelar: String?,                 // null = no se puede cancelar
    private val alEnviar: (PinPad, String, String) -> Unit,
    private val alCancelar: (PinPad) -> Unit
) {
    companion object {
        const val MIN_LARGO = 6
        const val MAX_LARGO = 12
    }

    val dialogo = Dialog(activity)

    private val azul = Color.parseColor("#2563EB")          // blue-600
    private val azulFoco = Color.parseColor("#60A5FA")      // blue-400
    private val fondoPantalla = activity.getColor(R.color.background)
    private val slate800 = Color.parseColor("#1E293B")
    private val slate700 = Color.parseColor("#334155")
    private val slate500 = Color.parseColor("#64748B")
    private val slate400 = Color.parseColor("#94A3B8")
    private val slate100 = Color.parseColor("#F1F5F9")
    private val slate50 = Color.parseColor("#F8FAFC")
    private val rosa = Color.parseColor("#F43F5E")          // rose-500
    private val negrita: Typeface? = ResourcesCompat.getFont(activity, R.font.quicksand_bold)
    private val media: Typeface? = ResourcesCompat.getFont(activity, R.font.quicksand_medium)

    private val tarjeta = LinearLayout(activity)
    private val tvError: TextView
    private val boton: TextView
    private val campos = ArrayList<EditText>()

    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    init {
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogo.setCancelable(false)

        tarjeta.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(32), dp(32), dp(24))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(40).toFloat()
            }
            elevation = dp(12).toFloat()
        }

        // Candado blanco sobre un cuadrado azul redondeado
        tarjeta.addView(ImageView(activity).apply {
            setImageResource(R.drawable.ic_lock)
            scaleType = ImageView.ScaleType.CENTER
            background = GradientDrawable().apply {
                setColor(azul)
                cornerRadius = dp(28).toFloat()
            }
            elevation = dp(6).toFloat()
        }, LinearLayout.LayoutParams(dp(80), dp(80)))

        tarjeta.addView(TextView(activity).apply {
            text = titulo
            textSize = 24f
            typeface = negrita
            setTextColor(slate800)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })

        tarjeta.addView(TextView(activity).apply {
            text = texto
            textSize = 14f
            typeface = media
            setTextColor(slate500)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Campos
        tarjeta.addView(
            campo(if (dosCampos) "PIN (mín. $MIN_LARGO números)" else "PIN", ultimo = !dosCampos),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
        )
        if (dosCampos) {
            tarjeta.addView(
                campo("Repetí el PIN", ultimo = true),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
            )
        }

        tvError = TextView(activity).apply {
            textSize = 12f
            typeface = negrita
            setTextColor(rosa)
            gravity = Gravity.CENTER
        }
        tarjeta.addView(tvError, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // Botón principal
        boton = TextView(activity).apply {
            text = textoBoton
            textSize = 16f
            typeface = negrita
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
                GradientDrawable().apply {
                    setColor(azul)
                    cornerRadius = dp(16).toFloat()
                },
                null
            )
            elevation = dp(4).toFloat()
            setOnClickListener { enviar() }
        }
        tarjeta.addView(boton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        if (textoCancelar != null) {
            tarjeta.addView(TextView(activity).apply {
                text = textoCancelar.uppercase()
                textSize = 11f
                typeface = negrita
                letterSpacing = 0.15f
                setTextColor(slate400)
                gravity = Gravity.CENTER
                setPadding(0, dp(14), 0, dp(6))
                setOnClickListener { alCancelar(this@PinPad) }
            }, LinearLayout.LayoutParams(-1, -2))
        }

        val envoltorio = FrameLayout(activity).apply {
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(tarjeta, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        }
        dialogo.setContentView(ScrollView(activity).apply {
            isFillViewport = true
            setBackgroundColor(fondoPantalla)
            addView(envoltorio, FrameLayout.LayoutParams(-1, -1))
        })
        dialogo.window?.apply {
            setBackgroundDrawable(ColorDrawable(fondoPantalla))
            setLayout(-1, -1)
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }
        actualizarBoton()
    }

    private fun fondoCampo(foco: Boolean) = GradientDrawable().apply {
        setColor(slate50)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(2), if (foco) azulFoco else slate100)
    }

    /** Un campo numérico oculto, con etiqueta chica arriba y ojito para mostrarlo. */
    private fun campo(etiqueta: String, ultimo: Boolean): View {
        val columna = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        columna.addView(TextView(activity).apply {
            text = etiqueta.uppercase()
            textSize = 10f
            typeface = negrita
            letterSpacing = 0.15f
            setTextColor(slate400)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })

        val oculto = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val visible = InputType.TYPE_CLASS_NUMBER
        val et = EditText(activity).apply {
            inputType = oculto
            typeface = negrita          // después del inputType: el modo contraseña lo cambia
            textSize = 18f
            letterSpacing = 0.15f
            setTextColor(slate700)
            filters = arrayOf(InputFilter.LengthFilter(MAX_LARGO))
            background = fondoCampo(false)
            setPadding(dp(16), dp(14), dp(52), dp(14))
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            imeOptions = if (ultimo) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            setOnFocusChangeListener { v, foco -> v.background = fondoCampo(foco) }
            setOnEditorActionListener { _, accion, _ ->
                if (accion == EditorInfo.IME_ACTION_DONE) { enviar(); true } else false
            }
            doAfterTextChanged {
                tvError.text = ""
                actualizarBoton()
            }
        }
        campos.add(et)

        val ojo = ImageView(activity).apply {
            setImageResource(R.drawable.ic_eye)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            contentDescription = "Mostrar u ocultar el PIN"
            setOnClickListener {
                val mostrar = et.inputType == oculto
                et.inputType = if (mostrar) visible else oculto
                et.typeface = negrita
                et.setSelection(et.text.length)
                setImageResource(if (mostrar) R.drawable.ic_eye_off else R.drawable.ic_eye)
            }
        }
        val caja = FrameLayout(activity)
        caja.addView(et, FrameLayout.LayoutParams(-1, -2))
        caja.addView(ojo, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(8)
        })
        columna.addView(caja, LinearLayout.LayoutParams(-1, -2))
        return columna
    }

    private fun actualizarBoton() {
        val listo = campos.all { it.text.isNotEmpty() }
        boton.isEnabled = listo
        boton.alpha = if (listo) 1f else 0.5f
    }

    private fun enviar() {
        if (!boton.isEnabled) return
        val primero = campos[0].text.toString()
        val segundo = if (dosCampos) campos[1].text.toString() else ""
        alEnviar(this, primero, segundo)
    }

    /** Vacía los campos y muestra el mensaje en rosa con una sacudida. */
    fun error(mensaje: String) {
        campos.forEach { it.setText("") }
        tvError.text = mensaje
        campos[0].requestFocus()
        ObjectAnimator.ofFloat(tarjeta, View.TRANSLATION_X, 0f, 16f).apply {
            duration = 350
            interpolator = CycleInterpolator(3f)
            start()
        }
    }

    fun mostrar() {
        dialogo.show()
        campos[0].requestFocus()
    }

    fun cerrar() {
        if (dialogo.isShowing) dialogo.dismiss()
    }
}
