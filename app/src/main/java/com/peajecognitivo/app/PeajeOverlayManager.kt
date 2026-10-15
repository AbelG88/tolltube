package com.peajecognitivo.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object PeajeOverlayManager {

    private var overlayView: View? = null

    fun show(context: Context) {
        if (overlayView != null) {
            PeajeLog.d("OVERLAY show() ignorado: ya hay un overlay marcado como visible")
            return
        }
        val horizontal =
            context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        PeajeLog.d("OVERLAY show() intentando mostrar (pantalla ${if (horizontal) "horizontal" else "vertical"})")

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val ejercicio = ExerciseGenerator.siguiente(context)
        PeajeLog.d("OVERLAY ejercicio: ${ejercicio.resumen}")

        val vista = LayoutInflater.from(context).inflate(R.layout.overlay_exercise, null)
        overlayView = vista

        val tvVisual = vista.findViewById<TextView>(R.id.tvVisual)
        val tvPrompt = vista.findViewById<TextView>(R.id.tvPrompt)
        val llNumeric = vista.findViewById<LinearLayout>(R.id.llNumeric)
        val llOptions = vista.findViewById<LinearLayout>(R.id.llOptions)

        tvVisual.text = ejercicio.visual
        tvPrompt.text = ejercicio.pregunta

        AudioPausa.pausar(context)
        Voz.hablar(context, ejercicio.hablado)
        var errores = 0

        val cerrarConExito = {
            Voz.callar()
            AudioPausa.soltar(context)
            try {
                windowManager.removeView(vista)
            } catch (e: Exception) {
                PeajeLog.d("OVERLAY error al quitar: ${e.javaClass.simpleName}: ${e.message}")
            }
            overlayView = null
            PeajeLog.d("OVERLAY respuesta correcta, overlay cerrado")
            SettingsStore.registrarResultado(context, ejercicio.tipo, errores)
            val total = SettingsStore.registrarAcierto(context)
            PeajeLog.d("OVERLAY aciertos de hoy: $total")
            Toast.makeText(context, "¡Muy bien! 🎉 ⭐ $total hoy", Toast.LENGTH_SHORT).show()
            // El tiempo hasta el próximo ejercicio se cuenta desde que se resolvió este
            ExerciseTimerManager.alCerrarEjercicio(context)
        }

        val opciones = ejercicio.opciones
        if (opciones == null) {
            // Respuesta numérica con teclado (sumas y restas)
            llNumeric.visibility = View.VISIBLE
            llOptions.visibility = View.GONE

            val etAnswer = vista.findViewById<EditText>(R.id.etAnswer)
            val btnSubmit = vista.findViewById<Button>(R.id.btnSubmit)
            etAnswer.text.clear()
            etAnswer.requestFocus()

            val verificar = {
                val respuesta = etAnswer.text.toString().trim()
                if (respuesta == ejercicio.correcta) {
                    cerrarConExito()
                } else {
                    errores++
                    PeajeLog.d("OVERLAY respuesta incorrecta: '$respuesta' (errores=$errores)")
                    etAnswer.text.clear()
                    if (errores >= 2) {
                        // Ayuda: después de dos errores se muestra la respuesta como pista
                        tvPrompt.text = "${ejercicio.pregunta}\n💡 Pista: la respuesta es ${ejercicio.correcta}"
                    } else {
                        etAnswer.error = "No es correcto. Probá de nuevo."
                    }
                }
            }

            btnSubmit.setOnClickListener { verificar() }
            etAnswer.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    verificar()
                    true
                } else false
            }
        } else {
            // Respuesta tocando una de 3 opciones (animales y objetos)
            llNumeric.visibility = View.GONE
            llOptions.visibility = View.VISIBLE

            val botones = listOf(
                vista.findViewById<Button>(R.id.btnOpt1),
                vista.findViewById<Button>(R.id.btnOpt2),
                vista.findViewById<Button>(R.id.btnOpt3)
            )
            botones.forEachIndexed { i, boton ->
                val texto = opciones[i]
                boton.text = texto
                boton.setOnClickListener {
                    if (texto == ejercicio.correcta) {
                        cerrarConExito()
                    } else {
                        errores++
                        PeajeLog.d("OVERLAY opción incorrecta: '$texto' (errores=$errores)")
                        boton.isEnabled = false
                        boton.alpha = 0.35f
                        if (errores >= 1) {
                            // Ayuda: tras el primer error se resalta la opción correcta en verde
                            botones.forEachIndexed { j, otro ->
                                if (opciones[j] == ejercicio.correcta) {
                                    otro.backgroundTintList =
                                        ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                                }
                            }
                        }
                        Toast.makeText(context, "Casi 🙂 Probá con otra", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        try {
            windowManager.addView(vista, params)
            PeajeLog.d("OVERLAY addView OK")
        } catch (e: Exception) {
            // OJO: comportamiento original sin cambios. overlayView queda asignado
            // aunque addView haya fallado, y la excepción se propaga.
            PeajeLog.d("OVERLAY addView FALLÓ: ${e.javaClass.simpleName}: ${e.message}")
            AudioPausa.soltar(context)
            throw e
        }
    }
}
