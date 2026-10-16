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

    /** Errores seguidos con opciones, sin acertar en el medio. Alarga la pausa de "pensá". */
    private var erroresSeguidos = 0

    /** Serie actual: cuántos ejercicios hay que resolver juntos y cuántos van. */
    private var serieTotal = 1
    private var serieHechos = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Pantalla obligatoria tras elegir una opción incorrecta: se lee la respuesta, hay que esperar
     * una cuenta regresiva y recién ahí aparece un ejercicio nuevo. Nunca desbloquea la pantalla.
     */
    private fun mostrarReflexion(
        context: Context,
        windowManager: WindowManager,
        vistaEjercicio: View,
        params: WindowManager.LayoutParams,
        ejercicio: Exercise
    ) {
        erroresSeguidos++
        val espera = minOf(2 + 3 * erroresSeguidos, 12)
        PeajeLog.d("OVERLAY reflexión por opción incorrecta (errores seguidos=$erroresSeguidos, espera=${espera}s)")

        val vista = LayoutInflater.from(context).inflate(R.layout.overlay_reflexion, null)
        vista.findViewById<TextView>(R.id.tvRefVisual).text = ejercicio.visual
        vista.findViewById<TextView>(R.id.tvRefTexto).text =
            "${ejercicio.pregunta}\n👉 ${ejercicio.correcta}"
        val tvCuenta = vista.findViewById<TextView>(R.id.tvRefCuenta)
        val btn = vista.findViewById<Button>(R.id.btnRefOk)
        btn.isEnabled = false

        try {
            windowManager.removeView(vistaEjercicio)
        } catch (e: Exception) {
            PeajeLog.d("OVERLAY error al quitar ejercicio: ${e.javaClass.simpleName}: ${e.message}")
        }
        overlayView = vista
        try {
            windowManager.addView(vista, params)
        } catch (e: Exception) {
            PeajeLog.d("OVERLAY reflexión addView FALLÓ: ${e.javaClass.simpleName}: ${e.message}")
            overlayView = null
            AudioPausa.soltar(context)
            throw e
        }
        Voz.hablar(
            context,
            "Esa no es. ${ejercicio.hablado} La respuesta es ${ejercicio.correcta}. Leé con atención."
        )

        var restante = espera
        val cuenta = object : Runnable {
            override fun run() {
                if (overlayView !== vista) return // se quitó por otro camino
                if (restante > 0) {
                    tvCuenta.text = "Podés seguir en $restante…"
                    restante--
                    handler.postDelayed(this, 1000)
                } else {
                    tvCuenta.text = "Listo"
                    btn.isEnabled = true
                }
            }
        }
        handler.post(cuenta)

        btn.setOnClickListener {
            Voz.callar()
            try {
                windowManager.removeView(vista)
            } catch (e: Exception) {
                PeajeLog.d("OVERLAY error al quitar reflexión: ${e.javaClass.simpleName}: ${e.message}")
            }
            overlayView = null
            PeajeLog.d("OVERLAY reflexión terminada, ejercicio nuevo")
            mostrar(context)
        }
    }

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
        PeajeLog.d("OVERLAY ejercicio: ${ejercicio.resumen}")

        val vista = LayoutInflater.from(context).inflate(R.layout.overlay_exercise, null)
        overlayView = vista

        val tvVisual = vista.findViewById<TextView>(R.id.tvVisual)
        val tvPrompt = vista.findViewById<TextView>(R.id.tvPrompt)
        val llNumeric = vista.findViewById<LinearLayout>(R.id.llNumeric)
        val llOptions = vista.findViewById<LinearLayout>(R.id.llOptions)

        if (serieTotal > 1) {
            vista.findViewById<TextView>(R.id.tvQuestion).text =
                "🛑 Peaje Cognitivo · ${serieHechos + 1} de $serieTotal"
        }
        tvVisual.text = ejercicio.visual
        tvPrompt.text = ejercicio.pregunta

        AudioPausa.pausar(context)
        Voz.hablar(context, ejercicio.hablado)
        var errores = 0

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
                Toast.makeText(context, "¡Muy bien! 🎉 ⭐ $total hoy", Toast.LENGTH_SHORT).show()
                // El tiempo hasta el próximo ejercicio se cuenta desde que se resolvió la serie
                ExerciseTimerManager.alCerrarEjercicio(context)
            }
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
                    SettingsStore.registrarError(context, ejercicio.tipo)
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
                        SettingsStore.registrarError(context, ejercicio.tipo)
                        PeajeLog.d("OVERLAY opción incorrecta: '$texto'")
                        mostrarReflexion(context, windowManager, vista, params, ejercicio)
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
