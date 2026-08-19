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
import android.widget.TextView
import android.widget.Toast

object PeajeOverlayManager {

    private var overlayView: View? = null

    fun show(context: Context) {
        if (overlayView != null) return

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

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_exercise, null)

        val tvQuestion = overlayView!!.findViewById<TextView>(R.id.tvQuestion)
        val etAnswer = overlayView!!.findViewById<EditText>(R.id.etAnswer)
        val btnSubmit = overlayView!!.findViewById<Button>(R.id.btnSubmit)

        tvQuestion.text = "🛑 Peaje Cognitivo\n\n¿Cuánto es 3 + 2?"
        etAnswer.text.clear()
        etAnswer.requestFocus()

        val checkAnswer = {
            val answer = etAnswer.text.toString().trim()
            if (answer == "5") {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {}
                overlayView = null
                Toast.makeText(context, "¡Muy bien! 🎉 Seguí viendo.", Toast.LENGTH_SHORT).show()
            } else {
                etAnswer.error = "No es correcto. Probá de nuevo."
                etAnswer.text.clear()
            }
        }

        btnSubmit.setOnClickListener { checkAnswer() }
        
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer()
                true
            } else false
        }

        windowManager.addView(overlayView, params)
    }
}
