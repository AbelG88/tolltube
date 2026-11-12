package com.peajecognitivo.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton

/** Un medio de aporte. Misma lista que la app Nappy. */
class EnlaceApoyo(
    val etiqueta: String,
    val url: String,
    val argentina: Boolean,
    val fondo: Int,
    val texto: Int
)

/**
 * Apoyo voluntario (misma lógica que Nappy):
 * - Se pide por aciertos: pasaron [CADA_ACIERTOS] aciertos desde el último pedido.
 * - O por ingresos: [CADA_INGRESOS] ingresos al panel de padres y al menos [MIN_ACIERTOS_POR_INGRESOS] aciertos.
 * - Nunca dos pedidos con menos de [ENFRIAMIENTO_DIAS] días. La fecha de instalación cuenta como el último pedido,
 *   así que los primeros días no se pide nada.
 * - "Ya doné" deja tranquilo [SILENCIO_DONE_DIAS] días.
 * El pedido se muestra SOLO en el panel de padres (con el PIN ya puesto), nunca sobre las apps del niño.
 */
object Donaciones {
    val ENLACES = listOf(
        EnlaceApoyo("Mercado Pago", "https://link.mercadopago.com.ar/abelg88", true, 0xFF009EE3.toInt(), Color.WHITE),
        EnlaceApoyo("Cafecito", "https://cafecito.app/abelg88", true, 0xFF0F766E.toInt(), Color.WHITE),
        EnlaceApoyo("PayPal", "https://paypal.me/AbelG88", false, 0xFF0070BA.toInt(), Color.WHITE),
        EnlaceApoyo("Buy Me a Coffee", "https://buymeacoffee.com/abelg88", false, 0xFFFFDD00.toInt(), 0xFF0F172A.toInt())
    )

    const val CADA_ACIERTOS = 10
    const val CADA_INGRESOS = 15
    const val MIN_ACIERTOS_POR_INGRESOS = 5
    const val ENFRIAMIENTO_DIAS = 7
    const val SILENCIO_DONE_DIAS = 90
    private const val DIA_MS = 24L * 60 * 60 * 1000
    private const val RETRASO_MS = 2500L

    private const val K_ULTIMO = "don_ultimo"
    private const val K_INGRESOS = "don_ingresos"
    private const val K_ACIERTOS_AL_PEDIR = "don_aciertos_al_pedir"
    private const val K_SILENCIO = "don_silencio_hasta"

    private var ingresoContado = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendiente: Runnable? = null
    private var abierto: AlertDialog? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("peaje_prefs", Context.MODE_PRIVATE)

    /** La primera vez, la fecha de instalación (ahora) cuenta como último pedido: respiro inicial. */
    private fun ultimo(context: Context): Long {
        val p = prefs(context)
        if (!p.contains(K_ULTIMO)) p.edit().putLong(K_ULTIMO, System.currentTimeMillis()).apply()
        return p.getLong(K_ULTIMO, System.currentTimeMillis())
    }

    /** Cuenta un ingreso por cada vez que se abre la app (no por cada vuelta a la pantalla). */
    fun contarIngreso(context: Context) {
        if (ingresoContado) return
        ingresoContado = true
        val p = prefs(context)
        p.edit().putInt(K_INGRESOS, p.getInt(K_INGRESOS, 0) + 1).apply()
    }

    private fun toca(context: Context, ahora: Long): Boolean {
        val p = prefs(context)
        if (ahora < p.getLong(K_SILENCIO, 0L)) return false
        if (ahora - ultimo(context) < ENFRIAMIENTO_DIAS * DIA_MS) return false
        val total = SettingsStore.aciertosTotal(context)
        if (total - p.getInt(K_ACIERTOS_AL_PEDIR, 0) >= CADA_ACIERTOS) return true
        return p.getInt(K_INGRESOS, 0) >= CADA_INGRESOS && total >= MIN_ACIERTOS_POR_INGRESOS
    }

    /**
     * Si toca, muestra el pedido tras una pausa corta. Llamar solo cuando el panel ya está
     * desbloqueado y sin otras ventanas encima.
     */
    fun mostrarSiToca(activity: Activity) {
        if (abierto?.isShowing == true || pendiente != null) return
        if (!toca(activity, System.currentTimeMillis())) return
        val tarea = object : Runnable { override fun run() {
            pendiente = null
            if (activity.isFinishing || activity.isDestroyed) return
            if (!PinGate.libre(activity)) { // el PIN sigue en pantalla: esperar a que lo pongan
                pendiente = this
                handler.postDelayed(this, RETRASO_MS)
                return
            }
            if (!toca(activity, System.currentTimeMillis())) return
            dialogo(activity)
        } }
        pendiente = tarea
        handler.postDelayed(tarea, RETRASO_MS)
    }

    fun cancelarPendiente() {
        pendiente?.let { handler.removeCallbacks(it) }
        pendiente = null
    }

    private fun alMostrar(context: Context) {
        prefs(context).edit()
            .putLong(K_ULTIMO, System.currentTimeMillis())
            .putInt(K_INGRESOS, 0)
            .putInt(K_ACIERTOS_AL_PEDIR, SettingsStore.aciertosTotal(context))
            .apply()
    }

    private fun alDonar(context: Context) {
        prefs(context).edit().putLong(K_SILENCIO, System.currentTimeMillis() + SILENCIO_DONE_DIAS * DIA_MS).apply()
    }

    private fun dialogo(activity: Activity) {
        alMostrar(activity)
        val total = SettingsStore.aciertosTotal(activity)
        val d = dp(activity, 1)
        val caja = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24 * d, 28 * d, 24 * d, 12 * d)
        }
        caja.addView(TextView(activity).apply {
            text = "❤️"
            textSize = 34f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(0xFFF43F5E.toInt()); cornerRadius = 28f * d }
            layoutParams = LinearLayout.LayoutParams(80 * d, 80 * d)
        })
        caja.addView(texto(activity, "¿Te sirve Peaje Cognitivo?", 22f, true, 18 * d))
        caja.addView(texto(
            activity,
            "Ya se resolvieron $total ${if (total == 1) "ejercicio" else "ejercicios"}. Es gratis y sin publicidad; " +
                "si te ayuda, un aporte me permite seguir mejorándola. Es totalmente opcional.",
            14f, false, 8 * d
        ).apply { setTextColor(activity.getColor(R.color.text_secondary)); setLineSpacing(0f, 1.2f) })
        caja.addView(botones(activity), LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 * d })

        val dlg = AlertDialog.Builder(activity)
            .setView(android.widget.ScrollView(activity).apply { addView(caja) })
            .setNeutralButton("Quizás más tarde", null)
            .setPositiveButton("Ya doné, ¡gracias! 💚") { _, _ -> alDonar(activity) }
            .create()
        abierto = dlg
        dlg.show()
        PeajeLog.d("DONACIONES pedido mostrado")
    }

    /** Botones de los medios de aporte, agrupados como en Nappy. Se usa en el pedido y en la pantalla principal. */
    fun botones(context: Context): LinearLayout {
        val d = dp(context, 1)
        val raiz = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        listOf(true to "Desde Argentina", false to "Desde el exterior").forEach { (ar, titulo) ->
            raiz.addView(texto(context, titulo, 12f, true, 8 * d).apply {
                setTextColor(context.getColor(R.color.text_hint)); gravity = Gravity.START
            })
            ENLACES.filter { it.argentina == ar }.forEach { e ->
                raiz.addView(MaterialButton(context).apply {
                    text = e.etiqueta
                    setTextColor(e.texto)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(e.fondo)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 * d }
                    setOnClickListener {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(e.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (ex: Exception) {
                            Toast.makeText(context, "No hay un navegador para abrir el enlace", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        }
        return raiz
    }

    private fun texto(context: Context, t: String, sp: Float, negrita: Boolean, arriba: Int) = TextView(context).apply {
        text = t
        textSize = sp
        gravity = Gravity.CENTER
        setTextColor(context.getColor(R.color.text_primary))
        if (negrita) setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = arriba }
    }

    private fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
