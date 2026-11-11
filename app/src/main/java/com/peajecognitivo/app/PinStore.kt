package com.peajecognitivo.app

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Resultado de comprobar un PIN. */
sealed class ResultadoPin {
    object Correcto : ResultadoPin()

    /** [intentosRestantes] = fallos que quedan antes del próximo bloqueo. */
    class Incorrecto(val intentosRestantes: Int) : ResultadoPin()

    class Bloqueado(val segundos: Long) : ResultadoPin()
}

/**
 * PIN de padres. Nunca se guarda el PIN: solo un hash PBKDF2 con sal aleatoria.
 * Los intentos fallidos y el bloqueo se guardan en disco, así que reiniciar la app no los borra.
 */
object PinStore {
    private const val PREFS = "peaje_prefs"
    private const val K_HASH = "pin_hash"
    private const val K_SAL = "pin_salt"
    private const val K_ITER = "pin_iter"
    private const val K_FALLOS = "pin_fallos"
    private const val K_BLOQUEO = "pin_bloqueo_hasta"
    private const val K_VIEJO = "pin" // versiones anteriores lo guardaban en texto plano

    private const val ITERACIONES = 120_000
    private const val FALLOS_LIBRES = 5
    private const val BLOQUEO_BASE_S = 30L
    private const val BLOQUEO_MAX_S = 15 * 60L

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hash(pin: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sal, iteraciones, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun deB64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    /** Si quedó un PIN viejo en texto plano, lo convierte a hash y borra el original. */
    @Synchronized
    private fun migrarSiHaceFalta(c: Context) {
        val p = prefs(c)
        val viejo = p.getString(K_VIEJO, null) ?: return
        if (viejo.isNotEmpty() && !p.contains(K_HASH)) guardar(c, viejo)
        p.edit().remove(K_VIEJO).apply()
    }

    @Synchronized
    fun tienePin(c: Context): Boolean {
        migrarSiHaceFalta(c)
        return prefs(c).contains(K_HASH)
    }

    @Synchronized
    fun guardar(c: Context, pin: String) {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(c).edit()
            .putString(K_HASH, b64(hash(pin, sal, ITERACIONES)))
            .putString(K_SAL, b64(sal))
            .putInt(K_ITER, ITERACIONES)
            .remove(K_VIEJO)
            .putInt(K_FALLOS, 0)
            .putLong(K_BLOQUEO, 0L)
            .apply()
    }

    @Synchronized
    fun verificar(c: Context, ingresado: String): ResultadoPin {
        migrarSiHaceFalta(c)
        val p = prefs(c)
        val ahora = System.currentTimeMillis()

        var hasta = p.getLong(K_BLOQUEO, 0L)
        // Si movieron el reloj hacia atrás, el bloqueo no puede pasar del máximo
        if (hasta - ahora > BLOQUEO_MAX_S * 1000) hasta = ahora + BLOQUEO_MAX_S * 1000
        if (ahora < hasta) return ResultadoPin.Bloqueado((hasta - ahora + 999) / 1000)

        val guardado = p.getString(K_HASH, null) ?: return ResultadoPin.Correcto // sin PIN
        val sal = deB64(p.getString(K_SAL, "") ?: "")
        val esperado = deB64(guardado)
        val calculado = hash(ingresado, sal, p.getInt(K_ITER, ITERACIONES))

        if (MessageDigest.isEqual(calculado, esperado)) {
            p.edit().putInt(K_FALLOS, 0).putLong(K_BLOQUEO, 0L).apply()
            return ResultadoPin.Correcto
        }

        val fallos = p.getInt(K_FALLOS, 0) + 1
        val edit = p.edit().putInt(K_FALLOS, fallos)
        if (fallos >= FALLOS_LIBRES) {
            val segundos = minOf(BLOQUEO_BASE_S shl (fallos - FALLOS_LIBRES).coerceAtMost(10), BLOQUEO_MAX_S)
            edit.putLong(K_BLOQUEO, ahora + segundos * 1000).apply()
            return ResultadoPin.Bloqueado(segundos)
        }
        edit.apply()
        return ResultadoPin.Incorrecto(FALLOS_LIBRES - fallos)
    }
}
