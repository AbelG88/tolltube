package com.peajecognitivo.app

import android.content.Context
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

    /** Errores seguidos sin acertar en el medio. Alarga la espera tras cada error. */
    private var erroresSeguidos = 0

    /** Serie actual: cuántos ejercicios hay que resolver juntos y cuántos van. */
    private var serieTotal = 1
    private var serieHechos = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private const val MSG_ERROR = "Esa no es. Probá de nuevo"

    /** Empieza una serie nueva (la que pide el timer o el botón de prueba). */
    fun show(context: Context) {
        if (overlayView != null) {
            PeajeLog.d("OVERLAY show() ignorado: ya hay un overlay marcado como visible")
            return
        }
        serieTotal = SettingsStore.getSerie(context)
        serieHechos = 0
        PeajeLog.d("OVERLAY serie nueva: $serieTotal ejercicio(s) seguidos")
        mostrar(context)
    }

    /** Muestra un ejercicio de la serie en curso. */
    private fun mostrar(context: Context) {
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
            // Con el teclado abierto, la pantalla se achica y el contenido se desplaza (no queda tapado)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val ejercicio = ExerciseGenerator.siguiente(context)
        PeajeLog.d("OVERLAY ejercicio de tipo ${ejercicio.tipo}")

        val vista = LayoutInflater.from(context).inflate(R.layout.overlay_exercise, null)
        overlayView = vista

        val tvVisual = vista.findViewById<TextView>(R.id.tvVisual)
        val tvPrompt = vista.findViewById<TextView>(R.id.tvPrompt)
        val tvError = vista.findViewById<TextView>(R.id.tvError)
        val llNumeric = vista.findViewById<LinearLayout>(R.id.llNumeric)
        val llOptions = vista.findViewById<LinearLayout>(R.id.llOptions)

        if (serieTotal > 1) {
            vista.findViewById<TextView>(R.id.tvQuestion).text =
                "Peaje Cognitivo · ${serieHechos + 1} de $serieTotal"
        }
        tvVisual.text = ejercicio.visual
        tvPrompt.text = ejercicio.pregunta

        AudioPausa.pausar(context)
        Voz.hablar(context, ejercicio.hablado)

        val cerrarConExito = {
            Voz.callar()
            try {
                windowManager.removeView(vista)
            } catch (e: Exception) {
                PeajeLog.d("OVERLAY error al quitar: ${e.javaClass.simpleName}: ${e.message}")
            }
            overlayView = null
            erroresSeguidos = 0
            SettingsStore.registrarResultado(context, ejercicio.tipo, 0)
            val total = SettingsStore.registrarAcierto(context)
            serieHechos++
            PeajeLog.d("OVERLAY respuesta correcta ($serieHechos de $serieTotal). Aciertos de hoy: $total")
            if (serieHechos < serieTotal) {
                // Faltan ejercicios de la serie: no se libera nada, sigue el siguiente
                Toast.makeText(context, "¡Bien! ⭐ $total hoy. Faltan ${serieTotal - serieHechos}", Toast.LENGTH_SHORT).show()
                mostrar(context)
            } else {
                AudioPausa.soltar(context)
                PeajeLog.d("OVERLAY serie completa, overlay cerrado")
                val n = SettingsStore.getNombreNino(context).let { if (it.isEmpty()) "" else ", $it" }
                Toast.makeText(context, "¡Muy bien$n! 🎉 ⭐ $total hoy", Toast.LENGTH_SHORT).show()
                // El tiempo hasta el próximo ejercicio se cuenta desde que se resolvió la serie
                ExerciseTimerManager.alCerrarEjercicio(context)
            }
        }

        // Una respuesta incorrecta SOLO avisa que está mal. Nunca muestra la respuesta ni pistas:
        // el ejercicio queda trabado hasta que se responde bien.
        val avisarError = {
            erroresSeguidos++
            SettingsStore.registrarError(context, ejercicio.tipo)
            PeajeLog.d("OVERLAY respuesta incorrecta (errores seguidos=$erroresSeguidos)")
            tvError.text = MSG_ERROR
            tvError.visibility = View.VISIBLE
            Voz.hablar(context, "$MSG_ERROR.")
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
                if (respuesta.isNotEmpty()) {
                    if (respuesta == ejercicio.correcta) {
                        cerrarConExito()
                    } else {
                        avisarError()
                        etAnswer.text.clear()
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
            var ordenActual: List<String> = opciones
            val pintar = {
                botones.forEachIndexed { i, boton -> boton.text = ordenActual[i] }
            }
            pintar()

            // Tras un error se traban los botones unos segundos (cada vez más) y las opciones
            // cambian de lugar, para que no se pueda ir tocando por descarte siempre en el mismo orden.
            val trabar = {
                botones.forEach { it.isEnabled = false }
                var restante = minOf(2 * erroresSeguidos, 10)
                val cuenta = object : Runnable {
                    override fun run() {
                        if (overlayView !== vista) return // se quitó por otro camino
                        if (restante > 0) {
                            tvError.text = "$MSG_ERROR en $restante…"
                            restante--
                            handler.postDelayed(this, 1000)
                        } else {
                            tvError.text = MSG_ERROR
                            ordenActual = ordenActual.shuffled()
                            pintar()
                            botones.forEach { it.isEnabled = true }
                        }
                    }
                }
                handler.post(cuenta)
            }

            botones.forEach { boton ->
                boton.setOnClickListener {
                    if (boton.text.toString() == ejercicio.correcta) {
                        cerrarConExito()
                    } else {
                        avisarError()
                        trabar()
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
