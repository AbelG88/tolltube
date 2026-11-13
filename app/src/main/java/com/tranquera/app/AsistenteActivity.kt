package com.tranquera.app

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
        val construir: (LinearLayout) -> Unit,
        val titulo: String,
        val subtitulo: String,
        val validar: () -> String? = { null }
    )

    // Respuestas (arrancan con lo que ya está configurado)
    private var nombre = ""
    private var edad = ""
    private val intereses = mutableSetOf<Interes>()
    private var sumasYRestas = true
    private var modoApps = ModoApps.SIEMPRE
    private var elegidas = setOf<String>()
    private var libres = setOf<String>()
    private var pin = ""

    /** true = el asistente agrega un niño nuevo (sin permisos ni PIN). */
    private var nuevo = false

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

        nuevo = intent.getBooleanExtra(EXTRA_NUEVO, false)
        if (!nuevo) {
            nombre = SettingsStore.getNombreNino(this)
            SettingsStore.getEdad(this).takeIf { it > 0 }?.let { edad = it.toString() }
            val tipos = SettingsStore.getTipos(this)
            sumasYRestas = ExerciseType.SUMA in tipos || ExerciseType.RESTA in tipos
            modoApps = SettingsStore.getModoApps(this)
            elegidas = SettingsStore.getAppsElegidas(this)
            libres = SettingsStore.getAppsLibres(this)
        } else {
            libres = Deteccion.LIBRES_POR_DEFECTO
        }

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

        // 1. Nombre y edad
        lista += Paso(
            titulo = if (nuevo) "¿Cómo se llama el nuevo peque?" else "¿Cómo se llama el peque?",
            subtitulo = "Con su edad elegimos el nivel de los ejercicios. Todo se guarda solo en este celular.",
            construir = { c ->
                c.addView(campo("Nombre o apodo", nombre, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, 30) { nombre = it.trim() })
                c.addView(campo("Edad", edad, InputType.TYPE_CLASS_NUMBER, 2) { edad = it.trim() }, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                    topMargin = dp(16)
                })
            },
            validar = {
                val e = edad.toIntOrNull()
                when {
                    nombre.isEmpty() -> "Poné un nombre o un apodo"
                    e == null || e !in 1..17 -> "Poné la edad (de 1 a 17)"
                    else -> null
                }
            }
        )

        // 2. Qué le gusta
        lista += Paso(
            titulo = "¿Qué le gusta?",
            subtitulo = "Con esto armamos los ejercicios. Podés marcar varios.",
            construir = { c ->
                val g = grupo(multiple = true)
                Interes.values().forEach { i ->
                    g.addView(chip(i.etiqueta, i in intereses) { v -> if (v) intereses += i else intereses -= i })
                }
                g.addView(chip("Sumas y restas", sumasYRestas) { v -> sumasYRestas = v })
                c.addView(g)
            },
            validar = {
                if (!sumasYRestas && intereses.isEmpty() && !ModulosStore.hayActivos(this))
                    "Elegí al menos una cosa" else null
            }
        )

        // 3. Qué apps cuidamos
        lista += Paso(
            titulo = "¿Qué apps cuidamos?",
            subtitulo = "Antes de abrirlas, el peque resuelve un ejercicio.",
            construir = { c -> construirApps(c) },
            validar = {
                if (modoApps == ModoApps.ELEGIR && elegidas.isEmpty()) "Elegí al menos una app" else null
            }
        )

        // 4. Permisos y PIN
        if (!nuevo) lista += Paso(
            titulo = if (PinStore.tienePin(this)) "Dos permisos" else "Dos permisos y un PIN",
            subtitulo = "Sin esto Android no nos deja cuidar. Son dos toques cada uno.",
            construir = { c -> construirPermisos(c) },
            validar = {
                if (!PinStore.tienePin(this) && pin.length < PinPad.MIN_LARGO) "Elegí un PIN de al menos ${PinPad.MIN_LARGO} números" else null
            }
        )
        return lista
    }

    private fun construirApps(c: LinearLayout) {
        val opciones = listOf(
            Triple(ModoApps.SIEMPRE, "Las de siempre", "YouTube, TikTok, Instagram y Netflix. Lo más simple."),
            Triple(ModoApps.ELEGIR, "Elijo yo cuáles", "Marcás de la lista de apps instaladas, juegos incluidos."),
            Triple(ModoApps.TODO, "Todo menos lo que permito", "Todo pide ejercicio, salvo lo que vos dejes libre.")
        )
        opciones.forEach { (modo, titulo, detalle) ->
            val marcado = modo == modoApps
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(getColor(if (marcado) R.color.surface else R.color.background))
                    cornerRadius = dp(22).toFloat()
                    setStroke(dp(2), getColor(if (marcado) R.color.accent else R.color.outline))
                }
                layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) }
                isClickable = true
                setOnClickListener {
                    modoApps = modo
                    if (modo == ModoApps.ELEGIR && elegidas.isEmpty()) elegidas = AppsInstaladas.deSiempreInstaladas(this@AsistenteActivity)
                    mostrarPaso()
                }
            }
            tarjeta.addView(texto((if (marcado) "● " else "○ ") + titulo, 17f, R.color.text_primary, bold = true))
            tarjeta.addView(texto(detalle, 13f, R.color.text_secondary, arriba = 2))
            c.addView(tarjeta)
        }
        when (modoApps) {
            ModoApps.SIEMPRE -> c.addView(texto(
                "Vamos a cuidar: " + AppVigilada.values().joinToString(", ") { it.nombre } + ".",
                14f, R.color.text_secondary, arriba = 6
            ))
            ModoApps.ELEGIR -> {
                c.addView(texto(AppsInstaladas.resumen(this, elegidas, "Todavía no elegiste ninguna"), 15f, R.color.text_primary, bold = true, arriba = 6))
                c.addView(botonSecundario("Elegir apps") {
                    AppsInstaladas.elegir(this, "Apps que cuidamos", elegidas) { elegidas = it; mostrarPaso() }
                })
            }
            ModoApps.TODO -> {
                c.addView(texto("Libres: " + AppsInstaladas.resumen(this, libres, "ninguna"), 15f, R.color.text_primary, bold = true, arriba = 6))
                c.addView(botonSecundario("Elegir cuáles quedan libres") {
                    AppsInstaladas.elegir(this, "Apps libres (sin ejercicio)", libres) { libres = it; mostrarPaso() }
                })
            }
        }
    }

    private fun construirPermisos(c: LinearLayout) {
        btnOverlay = botonPermiso {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btnUso = botonPermiso { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        c.addView(tarjeta(R.color.pastel_sky).apply {
            addView(texto("Mostrar el ejercicio encima", 17f, R.color.text_primary, bold = true))
            addView(texto("Para que aparezca sobre la app.", 13f, R.color.text_secondary, arriba = 2))
            addView(btnOverlay)
        })
        c.addView(tarjeta(R.color.pastel_mint).apply {
            addView(texto("Ver qué app está abierta", 17f, R.color.text_primary, bold = true))
            addView(texto("Para saber cuándo abre YouTube u otra app cuidada.", 13f, R.color.text_secondary, arriba = 2))
            addView(btnUso)
        })

        val ayuda = tarjeta(R.color.pastel_butter)
        val detalle = texto(
            "Pasa cuando la app no viene de Play Store. Se arregla una sola vez:\n" +
                "1. Tocá \"Abrir ajustes de la app\".\n" +
                "2. Arriba a la derecha, tocá ⋮.\n" +
                "3. Elegí \"Permitir configuración restringida\".\n" +
                "4. Volvé acá y tocá de nuevo el permiso.",
            14f, R.color.text_secondary, arriba = 8
        ).apply { setLineSpacing(0f, 1.25f) }
        val abrir = botonSecundario("Abrir ajustes de la app") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        detalle.visibility = View.GONE
        abrir.visibility = View.GONE
        val titulo = texto("¿Sale \"configuración restringida\"?  ▾", 15f, R.color.text_primary, bold = true).apply {
            isClickable = true
            setOnClickListener {
                val abrirAhora = detalle.visibility != View.VISIBLE
                detalle.visibility = if (abrirAhora) View.VISIBLE else View.GONE
                abrir.visibility = if (abrirAhora) View.VISIBLE else View.GONE
            }
        }
        ayuda.addView(titulo)
        ayuda.addView(detalle)
        ayuda.addView(abrir)
        c.addView(ayuda)

        if (!PinStore.tienePin(this)) {
            c.addView(texto("PIN para entrar a los ajustes", 17f, R.color.text_primary, bold = true, arriba = 8))
            c.addView(texto("Al menos ${PinPad.MIN_LARGO} números que el peque no sepa. Se elige en una pantalla aparte y se puede ver con el ojito.", 13f, R.color.text_secondary, arriba = 2))
            lateinit var btnPin: MaterialButton
            btnPin = botonSecundario(if (pin.length >= PinPad.MIN_LARGO) "✓ PIN elegido · Cambiar" else "Elegir PIN") {
                PinGate.elegirNuevo(this, obligatorio = false) { elegido ->
                    pin = elegido
                    btnPin.text = "✓ PIN elegido · Cambiar"
                }
            }
            c.addView(btnPin)
        }
        actualizarPermisos()
    }

    private fun mostrarPaso() {
        val p = pasos[actual]
        contenedor.removeAllViews()
        btnOverlay = null
        btnUso = null
        contenedor.addView(texto("Paso ${actual + 1} de ${pasos.size}", 14f, R.color.text_secondary))
        contenedor.addView(texto(p.titulo, 30f, R.color.text_primary, bold = true, arriba = 16))
        contenedor.addView(texto(p.subtitulo, 16f, R.color.text_secondary, arriba = 10))
        val cuerpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(24) }
        }
        p.construir(cuerpo)
        contenedor.addView(cuerpo)

        progreso.max = pasos.size
        progreso.setProgressCompat(actual + 1, true)
        btnAtras.visibility = if (actual == 0) View.INVISIBLE else View.VISIBLE
        btnSiguiente.text = if (actual == pasos.lastIndex) (if (nuevo) "Agregar" else "Empezar") else "Siguiente"
    }

    private fun terminar() {
        val primeraVez = !SettingsStore.onboardingHecho(this)
        if (nuevo) Ninos.setActivo(this, Ninos.crear(this)) // lo que sigue se guarda en el niño nuevo
        val edadNum = edad.toIntOrNull() ?: 0
        SettingsStore.setNombreNino(this, nombre)
        SettingsStore.setEdad(this, edadNum)
        SettingsStore.setNivel(this, SettingsStore.nivelParaEdad(edadNum))
        if (nuevo || primeraVez) SettingsStore.setModoCuidado(this, ModoCuidado.NORMAL)
        val tipos = buildSet {
            if (sumasYRestas) {
                add(ExerciseType.SUMA); add(ExerciseType.RESTA)
                if (edadNum <= 7) add(ExerciseType.CONTAR)
            }
        }
        SettingsStore.setTipos(this, tipos)
        SettingsStore.setModoApps(this, modoApps)
        when (modoApps) {
            ModoApps.SIEMPRE -> AppVigilada.values().forEach { SettingsStore.setAppVigilada(this, it, true) }
            ModoApps.ELEGIR -> SettingsStore.setAppsElegidas(this, elegidas)
            ModoApps.TODO -> SettingsStore.setAppsLibres(this, libres)
        }
        val instalados = CatalogoModulos.aplicar(this, intereses)
        if (!PinStore.tienePin(this)) {
            PinStore.guardar(this, pin)
            PinGate.desbloquearAhora()
        }
        SettingsStore.setOnboardingHecho(this, true)
        ExerciseTimerManager.reprogramar(this)
        TranqueraLog.d("ASISTENTE terminado: modo apps ${modoApps.clave}, $instalados módulos, nivel ${SettingsStore.getNivel(this)}")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ---------- Piezas de pantalla ----------

    private fun actualizarPermisos() {
        estadoBoton(btnOverlay, Settings.canDrawOverlays(this))
        estadoBoton(btnUso, UsoApps.tienePermiso(this))
    }

    private fun estadoBoton(b: MaterialButton?, ok: Boolean) {
        b ?: return
        b.text = if (ok) "✓ Listo" else "Activar"
        b.alpha = if (ok) 0.65f else 1f
    }

    private fun botonPermiso(alTocar: () -> Unit) =
        MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
            setOnClickListener { alTocar() }
        }

    /** Tarjeta pastel redondeada para agrupar contenido. */
    private fun tarjeta(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(18))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(getColor(color))
            cornerRadius = dp(28).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(14) }
    }

    private fun campo(pista: String, valor: String, tipo: Int, max: Int, alCambiar: (String) -> Unit): TextInputLayout {
        val capa = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = pista
            setBoxCornerRadii(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat())
        }
        val campo = TextInputEditText(capa.context).apply {
            inputType = tipo
            isSingleLine = true
            filters = arrayOf(InputFilter.LengthFilter(max))
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            setText(valor)
            doAfterTextChanged { alCambiar(it?.toString() ?: "") }
        }
        capa.addView(campo, LinearLayout.LayoutParams(MATCH, WRAP))
        return capa
    }

    private fun botonSecundario(t: String, alTocar: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = t
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) }
            setOnClickListener { alTocar() }
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

    companion object {
        const val EXTRA_NUEVO = "nuevo"
        private const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
