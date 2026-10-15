package com.peajecognitivo.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.KeyEvent

/**
 * Pausa el video mientras está el ejercicio. Dos recursos a la vez:
 * 1. Pide el foco de audio (YouTube se pausa cuando otra app lo toma).
 * 2. Manda la tecla "pausa" de medios (solo pausa; nunca manda "play").
 * Al cerrar el ejercicio se devuelve el foco; el video queda pausado o se reanuda según YouTube.
 */
object AudioPausa {
    private var pedido: AudioFocusRequest? = null

    fun pausar(context: Context) {
        if (pedido != null) return // ya está pausado (por ejemplo, al reintentar tras un error)
        try {
            val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val solicitud = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
            val r = am.requestAudioFocus(solicitud)
            pedido = solicitud
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            PeajeLog.d("AUDIO pausa pedida (foco=${if (r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "concedido" else "no concedido"})")
        } catch (e: Exception) {
            PeajeLog.d("AUDIO error al pausar: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun soltar(context: Context) {
        try {
            val solicitud = pedido ?: return
            val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocusRequest(solicitud)
            pedido = null
            PeajeLog.d("AUDIO foco devuelto")
        } catch (e: Exception) {
            PeajeLog.d("AUDIO error al soltar: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
