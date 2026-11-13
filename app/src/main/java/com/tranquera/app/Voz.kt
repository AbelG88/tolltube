package com.tranquera.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lee el ejercicio en voz alta (TextToSpeech del sistema). Apagado por defecto. */
object Voz {
    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null

    fun hablar(context: Context, texto: String) {
        if (!SettingsStore.getVoz(context)) return
        val actual = tts
        if (actual != null && listo) {
            actual.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tranquera")
            return
        }
        pendiente = texto
        if (actual != null) return // se está inicializando
        tts = TextToSpeech(context.applicationContext) { estado ->
            val motor = tts
            if (estado == TextToSpeech.SUCCESS && motor != null) {
                val r = motor.setLanguage(Locale("es", "AR"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    motor.setLanguage(Locale("es"))
                }
                listo = true
                val texto2 = pendiente
                pendiente = null
                if (texto2 != null) motor.speak(texto2, TextToSpeech.QUEUE_FLUSH, null, "tranquera")
                TranqueraLog.d("VOZ lista")
            } else {
                TranqueraLog.d("VOZ no disponible (estado=$estado)")
                tts = null
            }
        }
    }

    fun callar() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            TranqueraLog.d("VOZ error al callar: ${e.message}")
        }
    }
}
