package com.peajecognitivo.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Asistente de configuración: una pregunta por pantalla. Corre la primera vez que se abre la app
 * y se puede repetir desde la pantalla principal. Nada se guarda hasta el último paso.
 */
class AsistenteActivity : AppCompatActivity() {

    private class Paso(
        val titulo: String,
        val subtitulo: String,
        val construir: (LinearLayout) -> Unit = {},
        val validar: () -> String? = { null }
    )

    // Respuestas (arrancan con lo que ya está configurado)
    private var nombre = ""
    private val intereses = mutableSetOf<Interes>()
    private val tipos = mutableSetOf<ExerciseType>()
    private var nivel = 2
    private var intervaloSeg = 600
    private var serie = 1
    private val apps = mutableSetOf<AppVigilada>()
    private var pin = ""
    private var pinRepetido = ""

    private lateinit var pasos: List<Paso>
    private var actual = 0
    private lateinit var contenedor: LinearLayout
    private lateinit var progreso: LinearProgressIndicator
    private lateinit var btnSiguiente: MaterialButton
    private lateinit var btnAtras: MaterialButton
    private var btnOverlay: MaterialButton? = null
    private var btnUso: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistente)
        contenedor = findViewById(R.id.contenedorPaso)
        progreso = findViewById(R.id.progreso)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnAtras = findViewById(R.id.btnAtras)

        nombre = SettingsStore.getNombreNino(this)
        nivel = SettingsStore.getNivel(this)
        serie = SettingsStore.getSerie(this)
        tipos += SettingsStore.getTipos(this)
        AppVigilada.values().filter { SettingsStore.appVigilada(this, it) }.forEach { apps += it }
        if (SettingsStore.onboardingHecho(this)) intervaloSeg = SettingsStore.getIntervalSeconds(this)

        pasos = armarPasos()

        btnSiguiente.setOnClickListener {
            ocultarTeclado()
            val error = pasos[actual].validar()
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            } else if (actual == pasos.lastIndex) {
                terminar()
            } else {
                actual++
                mostrarPaso()
            }
        }
        btnAtras.setOnClickListener { atras() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = atras()
        })

        mostrarPaso()
    }

    override fun onResume() {
        super.onResume()
        // Si ya hay configuración hecha, repetir el asistente pide el PIN
        if (SettingsStore.onboardingHecho(this)) PinGate.exigir(this)
        actualizarPermisos()
    }

    private fun atras() {
        if (actual > 0) {
            ocultarTeclado()
            actual--
            mostrarPaso()
        } else {
            finish()
        }
    }

    // ---------- Pasos ----------

    private fun armarPasos(): List<Paso> {
        val lista = mutableListOf<Paso>()

        lista += Paso(
            "Armemos el peaje",
            "Unas preguntas rápidas y queda listo. Después podés cambiar todo desde la app."
        )

        lista += Paso(
            "¿Cómo se llama?",
            "Solo se guarda en este celular. Alcanza con un apodo.",
            construir = { c ->
                val capa = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                    hint = "Nombre del niño"
                    setBoxCornerRadii(dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat())
                }
                val campo = TextInputEditText(capa.context).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    isSingleLine = true
                    filters = arrayOf(InputFilter.LengthFilter(30))
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                    setText(nombre)
                    doAfterTextChanged { nombre = it?.toString()?.trim() ?: "" }
                }
                capa.addView(campo, LinearLayout.LayoutParams(MATCH, WRAP))
                c.addView(capa, LinearLayout.LayoutParams(MATCH, WRAP))
            }
        )

        lista += Paso(
            "¿Qué le gusta?",
            "Con eso elegimos los módulos de ejercicios. Podés marcar varios.",
            construir = { c ->
                val g = grupo(multiple = true)
                Interes.values().forEach { i ->
                    g.addView(chip(i.etiqueta, i in intereses) { v ->
                        if (v) { intereses += i } else { intereses -= i }
                    })
                }
                c.addView(g)
            }
        )

        lista += Paso(
            "¿Practicamos cuentas?",
            "Sumas, restas y contar, con dibujos.",
            construir = { c ->
                val g = grupo(multiple = true)
                listOf(
                    ExerciseType.SUMA to "Sumas",
                    ExerciseType.RESTA to "Restas",
                    ExerciseType.CONTAR to "Contar"
                ).forEach { (t, etiqueta) ->
                    g.addView(chip(etiqueta, t in tipos) { v ->
                        if (v) { tipos += t } else { tipos -= t }
                    })
                }
                c.addView(g)
            },
            validar = {
                if (tipos.isEmpty() && intereses.isEmpty() && !ModulosStore.hayActivos(this))
                    "Elegí al menos un tema o un tipo de cuenta" else null
            }
        )

        lista += Paso(
            "¿Qué nivel?",
            "Se puede cambiar cuando quieras.",
            construir = { c ->
                val g = grupo(multiple = false)
                listOf(1 to "Fácil", 2 to "Medio", 3 to "Difícil").forEach { (n, etiqueta) ->
                    g.addView(chip(etiqueta, n == nivel) { v -> if (v) nivel = n })
                }
                c.addView(g)
            }
        )

        lista += Paso(
            "¿Cada cuánto aparece un ejercicio?",
            "Es el tiempo mirando la app, entre un ejercicio y el siguiente.",
            construir = { c ->
                val g = grupo(multiple = false)
                (listOf(300, 600, 900, 1200, 1800) + intervaloSeg).distinct().sorted().forEach { seg ->
                    g.addView(chip(SettingsStore.formatear(seg), seg == intervaloSeg) { v -> if (v) intervaloSeg = seg })
                }
                c.addView(g)
            }
        )

        lista += Paso(
            "¿Cuántos ejercicios por vez?",
            "Cuántos tiene que resolver seguidos cada vez que aparece.",
            construir = { c ->
                val g = grupo(multiple = false)
                (listOf(1, 2, 3, 4, 5) + serie).distinct().sorted().forEach { n ->
                    g.addView(chip(n.toString(), n == serie) { v -> if (v) serie = n })
                }
                c.addView(g)
            }
        )

        lista += Paso(
            "¿Qué apps vigilamos?",
            "El ejercicio aparece sobre las que marques.",
            construir = { c ->
                val g = grupo(multiple = true)
                AppVigilada.values().forEach { a ->
                    g.addView(chip(a.nombre, a in apps) { v ->
                        if (v) { apps += a } else { apps -= a }
                    })
                }
                c.addView(g)
            },
            validar = { if (apps.isEmpty()) "Elegí al menos una app" else null }
        )

        lista += Paso(
            "Dos permisos",
            "Sirven para mostrar el ejercicio sobre otras apps y saber cuál está abierta. No se envía nada a ningún lado.",
            construir = { c ->
                btnOverlay = botonPermiso {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
                btnUso = botonPermiso { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                c.addView(btnOverlay)
                c.addView(btnUso)
                actualizarPermisos()
            }
        )

        if (!PinStore.tienePin(this)) {
            lista += Paso(
                "Poné un PIN de padres",
                "Protege los ajustes y el botón de apagar. De 4 a 6 números.",
                construir = { c ->
                    c.addView(campoPin("PIN") { pin = it })
                    c.addView(campoPin("Repetir el PIN") { pinRepetido = it }, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                        topMargin = dp(12)
                    })
                },
                validar = {
                    when {
                        pin.length !in 4..6 -> "El PIN tiene que tener de 4 a 6 números"
                        pin != pinRepetido -> "Los PIN no coinciden"
                        else -> null
                    }
                }
            )
        }

        lista += Paso(
            "Todo listo",
            "Esto es lo que va a quedar. Lo podés cambiar desde la app cuando quieras.",
            construir = { c ->
                val niveles = listOf("Fácil", "Medio", "Difícil")
                val lineas = buildList {
                    if (nombre.isNotEmpty()) add("Para $nombre")
                    add("Un ejercicio cada ${SettingsStore.formatear(intervaloSeg)} · $serie por vez")
                    add("Nivel: ${niveles[nivel - 1]}")
                    add("Apps: " + apps.joinToString(", ") { it.nombre })
                    if (intereses.isNotEmpty()) add("Temas: " + intereses.joinToString(", ") { it.etiqueta })
                }
                lineas.forEach { c.addView(texto(it, 18f, R.color.text_primary, arriba = 10)) }
            }
        )
        return lista
    }

    private fun mostrarPaso() {
        val p = pasos[actual]
        contenedor.removeAllViews()
        btnOverlay = null
        btnUso = null
        contenedor.addView(texto(p.titulo, 30f, R.color.text_primary, bold = true))
        contenedor.addView(texto(p.subtitulo, 16f, R.color.text_secondary, arriba = 10))
        val cuerpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(32) }
        }
        p.construir(cuerpo)
        contenedor.addView(cuerpo)

        progreso.max = pasos.size - 1
        progreso.setProgressCompat(actual, true)
        btnAtras.visibility = if (actual == 0) View.INVISIBLE else View.VISIBLE
        btnSiguiente.text = when (actual) {
            0 -> "Comenzar"
            pasos.lastIndex -> "Empezar"
            else -> "Siguiente"
        }
    }

    private fun terminar() {
        SettingsStore.setNombreNino(this, nombre)
        SettingsStore.setNivel(this, nivel)
        SettingsStore.setIntervalSeconds(this, intervaloSeg)
        SettingsStore.setSerie(this, serie)
        SettingsStore.setTipos(this, tipos.toSet())
        AppVigilada.values().forEach { SettingsStore.setAppVigilada(this, it, it in apps) }
        val instalados = CatalogoModulos.instalar(this, intereses)
        if (!PinStore.tienePin(this)) {
            PinStore.guardar(this, pin)
            PinGate.desbloquearAhora()
        }
        SettingsStore.setOnboardingHecho(this, true)
        ExerciseTimerManager.reprogramar(this)
        PeajeLog.d("ASISTENTE terminado: ${apps.size} apps, $instalados módulos instalados, nivel $nivel, cada ${intervaloSeg}s, serie $serie")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ---------- Piezas de pantalla ----------

    private fun actualizarPermisos() {
        btnOverlay?.text =
            if (Settings.canDrawOverlays(this)) "✓ Dibujar sobre otras apps" else "Permitir: dibujar sobre otras apps"
        btnUso?.text =
            if (UsoApps.tienePermiso(this)) "✓ Acceso a datos de uso" else "Permitir: acceso a datos de uso"
    }

    private fun botonPermiso(alTocar: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            minHeight = dp(56)
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(12) }
            setOnClickListener { alTocar() }
        }

    private fun campoPin(pista: String, alCambiar: (String) -> Unit): TextInputLayout {
        val capa = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = pista
            setBoxCornerRadii(dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat())
        }
        val campo = TextInputEditText(capa.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(6))
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            doAfterTextChanged { alCambiar(it?.toString() ?: "") }
        }
        capa.addView(campo, LinearLayout.LayoutParams(MATCH, WRAP))
        return capa
    }

    private fun grupo(multiple: Boolean) = ChipGroup(this).apply {
        isSingleSelection = !multiple
        isSelectionRequired = !multiple
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun chip(texto: String, marcado: Boolean, alCambiar: (Boolean) -> Unit) = Chip(this).apply {
        id = View.generateViewId()
        text = texto
        isCheckable = true
        isChecked = marcado
        setOnCheckedChangeListener { _, v -> alCambiar(v) }
    }

    private fun texto(t: String, sp: Float, color: Int, bold: Boolean = false, arriba: Int = 0) =
        TextView(this).apply {
            text = t
            textSize = sp
            setTextColor(getColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(arriba) }
        }

    private fun ocultarTeclado() {
        currentFocus?.let {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
