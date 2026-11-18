package com.tranquera.app

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Desde Android 15 (nivel 35) las pantallas se dibujan debajo de la barra de estado y de la barra de gestos.
 * Esto deja el contenido adentro de esas barras (padding) y pinta el fondo de la app detrás.
 * Se llama una vez en onCreate, después de super.onCreate().
 */
fun Activity.ajustarBordes() {
    window.setBackgroundDrawable(ColorDrawable(getColor(R.color.background)))
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { vista, insets ->
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        vista.findViewById<View>(android.R.id.content)?.setPadding(b.left, b.top, b.right, b.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
