package com.tranquera.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Armar un módulo propio: nombre, preguntas con su respuesta correcta (y 2 incorrectas si es de
 * elegir opción, o un número si se responde con el teclado). Al guardar, el módulo se suma a la
 * lista y se muestra su JSON para copiarlo o guardarlo como archivo e importarlo en otro celu.
 */
class CrearModuloActivity : AppCompatActivity() {

    private val preguntas = ArrayList<ItemModulo>()
    private lateinit var etNombre: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etEdad: TextInputEditText
    private lateinit var lista: LinearLayout
    private lateinit var tvVacio: TextView
    private var jsonPendiente: String? = null
    private var guardado = false

    private val guardarArchivo =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            val json = jsonPendiente
            if (uri != null && json != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    Toast.makeText(this, "Archivo guardado ✓", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo guardar el archivo", Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Crear mi módulo"

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        raiz.addView(texto("Crear mi módulo", 26f, R.color.text_primary, negrita = true))
        raiz.addView(
            texto(
                "Armá tu propio quiz: escribí cada pregunta con su respuesta correcta. Al guardar, el módulo se suma " +
                    "solo a tus ejercicios y te damos el archivo .json por si querés importarlo en otro celu.",
                14f, R.color.text_secondary, arriba = 6
            )
        )

        val (lNombre, eNombre) = campo("Nombre del módulo", 40)
        etNombre = eNombre
        raiz.addView(lNombre, params(arriba = 20))
        val (lDesc, eDesc) = campo("Descripción (opcional)", 80)
        etDescripcion = eDesc
        raiz.addView(lDesc, params(arriba = 10))
        val (lEdad, eEdad) = campo("Edad sugerida (opcional, ej: 4 a 6 años)", 20)
        etEdad = eEdad
        raiz.addView(lEdad, params(arriba = 10))

        raiz.addView(texto("Preguntas", 18f, R.color.text_primary, negrita = true, arriba = 24))
        tvVacio = texto("Todavía no cargaste ninguna pregunta.", 14f, R.color.text_hint, arriba = 6)
        raiz.addView(tvVacio)
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        raiz.addView(lista)

        raiz.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "➕ Agregar pregunta"
            setOnClickListener { editarPregunta(null) }
        }, params(arriba = 12))

        raiz.addView(MaterialButton(this).apply {
            text = "Guardar módulo"
            minimumHeight = dp(52)
            setOnClickListener { guardar() }
        }, params(arriba = 24))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.background))
            isFillViewport = true
            addView(raiz)
        })

        refrescarLista()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val hayAlgo = preguntas.isNotEmpty() || !etNombre.text.isNullOrBlank()
                if (!hayAlgo || guardado) {
                    finish()
                    return
                }
                AlertDialog.Builder(this@CrearModuloActivity)
                    .setTitle("¿Salir sin guardar?")
                    .setMessage("Se pierde lo que cargaste.")
                    .setPositiveButton("Salir") { _, _ -> finish() }
                    .setNegativeButton("Seguir armando", null)
                    .show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        PinGate.exigir(this)
    }

    // ---------- Pantalla ----------

    private fun texto(
        t: String, tam: Float, color: Int, negrita: Boolean = false, arriba: Int = 0
    ) = TextView(this).apply {
        text = t
        textSize = tam
        setTextColor(getColor(color))
        if (negrita) setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(arriba) }
    }

    /** Texto tocable, con el color de acento (como un botón de texto). */
    private fun enlace(t: String, alTocar: () -> Unit) = TextView(this).apply {
        text = t
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(getColor(R.color.accent))
        setPadding(dp(16), dp(10), dp(8), dp(10))
        setOnClickListener { alTocar() }
    }

    private fun params(arriba: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(arriba) }

    private fun campo(
        pista: String, max: Int,
        tipo: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    ): Pair<TextInputLayout, TextInputEditText> {
        val capa = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = pista
            setBoxCornerRadii(dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat())
        }
        val et = TextInputEditText(capa.context).apply {
            inputType = tipo
            filters = arrayOf(InputFilter.LengthFilter(max))
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        capa.addView(et, LinearLayout.LayoutParams(-1, -2))
        return capa to et
    }

    private fun refrescarLista() {
        lista.removeAllViews()
        tvVacio.visibility = if (preguntas.isEmpty()) View.VISIBLE else View.GONE
        preguntas.forEachIndexed { i, p -> lista.addView(filaPregunta(i, p)) }
    }

    private fun filaPregunta(i: Int, p: ItemModulo): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            background = GradientDrawable().apply {
                setColor(getColor(R.color.pastel_sky))
                cornerRadius = dp(20).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        }
        val encabezado = (if (p.visual.isNotEmpty()) "${p.visual}  " else "") + p.pregunta
        fila.addView(texto("${i + 1}. $encabezado", 15f, R.color.text_primary, negrita = true))
        fila.addView(texto("✓ ${p.correcta}", 14f, R.color.text_secondary, arriba = 2))
        if (p.tipo == "opciones") {
            fila.addView(texto("✗ ${p.incorrectas.joinToString("  ·  ")}", 13f, R.color.text_hint))
        } else {
            fila.addView(texto("Se responde con el teclado", 13f, R.color.text_hint))
        }
        val botones = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }
        botones.addView(enlace("Editar") { editarPregunta(i) })
        botones.addView(enlace("Quitar") {
            preguntas.removeAt(i)
            refrescarLista()
        })
        fila.addView(botones)
        return fila
    }

    // ---------- Agregar / editar una pregunta ----------

    private fun editarPregunta(indice: Int?) {
        if (indice == null && preguntas.size >= ModulosStore.MAX_EJERCICIOS) {
            Toast.makeText(this, "Máximo ${ModulosStore.MAX_EJERCICIOS} preguntas por módulo", Toast.LENGTH_LONG).show()
            return
        }
        val actual = indice?.let { preguntas[it] }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(0))
        }
        val grupoTipo = ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        val chOpciones = Chip(this).apply { text = "Elegir opción"; isCheckable = true }
        val chNumero = Chip(this).apply { text = "Responder con número"; isCheckable = true }
        grupoTipo.addView(chOpciones)
        grupoTipo.addView(chNumero)
        contenido.addView(grupoTipo)

        val (lVisual, eVisual) = campo("Dibujo o emoji (opcional)", 40)
        val (lPregunta, ePregunta) = campo("Pregunta", ModulosStore.MAX_TEXTO)
        val (lCorrecta, eCorrecta) = campo("Respuesta correcta", ModulosStore.MAX_TEXTO)
        val (lInc1, eInc1) = campo("Respuesta incorrecta 1", ModulosStore.MAX_TEXTO)
        val (lInc2, eInc2) = campo("Respuesta incorrecta 2", ModulosStore.MAX_TEXTO)
        contenido.addView(lVisual, params(arriba = 8))
        contenido.addView(lPregunta, params(arriba = 8))
        contenido.addView(lCorrecta, params(arriba = 8))
        contenido.addView(lInc1, params(arriba = 8))
        contenido.addView(lInc2, params(arriba = 8))

        fun aplicarTipo(opciones: Boolean) {
            lInc1.visibility = if (opciones) View.VISIBLE else View.GONE
            lInc2.visibility = if (opciones) View.VISIBLE else View.GONE
            lCorrecta.hint = if (opciones) "Respuesta correcta" else "Respuesta correcta (un número)"
            eCorrecta.inputType = if (opciones) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            } else {
                InputType.TYPE_CLASS_NUMBER
            }
        }
        chOpciones.setOnCheckedChangeListener { _, marcado -> if (marcado) aplicarTipo(true) }
        chNumero.setOnCheckedChangeListener { _, marcado -> if (marcado) aplicarTipo(false) }

        if (actual != null) {
            eVisual.setText(actual.visual)
            ePregunta.setText(actual.pregunta)
            eCorrecta.setText(actual.correcta)
            if (actual.tipo == "opciones") {
                eInc1.setText(actual.incorrectas.getOrNull(0) ?: "")
                eInc2.setText(actual.incorrectas.getOrNull(1) ?: "")
                chOpciones.isChecked = true
            } else {
                chNumero.isChecked = true
            }
        } else {
            chOpciones.isChecked = true
        }
        aplicarTipo(chOpciones.isChecked)

        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (actual == null) "Nueva pregunta" else "Editar pregunta")
            .setView(ScrollView(this).apply { addView(contenido) })
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                listOf(lPregunta, lCorrecta, lInc1, lInc2).forEach { it.error = null }
                val opciones = chOpciones.isChecked
                val pregunta = ePregunta.text.toString().trim()
                val correcta = eCorrecta.text.toString().trim()
                val visual = eVisual.text.toString().trim()
                val incorrectas = listOf(eInc1, eInc2)
                    .map { it.text.toString().trim() }
                    .filter { it.isNotEmpty() && it != correcta }
                    .distinct()
                var ok = true
                if (pregunta.isEmpty()) { lPregunta.error = "Escribí la pregunta"; ok = false }
                if (correcta.isEmpty()) { lCorrecta.error = "Escribí la respuesta correcta"; ok = false }
                if (ok && !opciones && !Regex("^\\d{1,6}$").matches(correcta)) {
                    lCorrecta.error = "Tiene que ser un número (hasta 6 cifras)"
                    ok = false
                }
                if (ok && opciones && incorrectas.size < 2) {
                    lInc2.error = "Hacen falta 2 incorrectas distintas entre sí y de la correcta"
                    ok = false
                }
                if (!ok) return@setOnClickListener
                val item = ItemModulo(
                    tipo = if (opciones) "opciones" else "numero",
                    visual = visual,
                    pregunta = pregunta,
                    hablado = null,
                    correcta = correcta,
                    incorrectas = if (opciones) incorrectas else emptyList()
                )
                if (indice == null) preguntas.add(item) else preguntas[indice] = item
                refrescarLista()
                dialogo.dismiss()
            }
        }
        dialogo.show()
    }

    // ---------- Guardar ----------

    private fun guardar() {
        val nombre = etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ponele un nombre al módulo", Toast.LENGTH_SHORT).show()
            etNombre.requestFocus()
            return
        }
        if (preguntas.isEmpty()) {
            Toast.makeText(this, "Agregá al menos una pregunta", Toast.LENGTH_SHORT).show()
            return
        }
        val json = ModulosStore.construirJson(
            nombre, etDescripcion.text.toString(), etEdad.text.toString(), preguntas
        )
        val m = try {
            ModulosStore.parsear(json)
        } catch (e: IllegalArgumentException) {
            AlertDialog.Builder(this)
                .setTitle("No se pudo guardar")
                .setMessage(e.message)
                .setPositiveButton("Entendido", null)
                .show()
            return
        }
        if (ModulosStore.listar(this).any { it.id == m.id }) {
            AlertDialog.Builder(this)
                .setTitle("Ya existe un módulo con ese nombre")
                .setMessage("¿Lo reemplazamos por este?")
                .setPositiveButton("Reemplazar") { _, _ -> sumar(json) }
                .setNegativeButton("Cambiar el nombre", null)
                .show()
        } else {
            sumar(json)
        }
    }

    private fun sumar(json: String) {
        try {
            val m = ModulosStore.importar(this, json)
            guardado = true
            jsonPendiente = json
            TranqueraLog.d("MODULOS creado '${m.nombre}' (${m.items.size} preguntas)")
            mostrarResultado(m, json)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar el módulo", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarResultado(m: Modulo, json: String) {
        val vista = TextView(this).apply {
            text = json
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Módulo listo ✓")
            .setMessage(
                "\"${m.nombre}\" ya está en tus módulos (${m.items.size} preguntas). Este es su archivo, " +
                    "por si querés importarlo en otro celu:"
            )
            .setView(ScrollView(this).apply { addView(vista) })
            .setPositiveButton("Listo") { _, _ -> finish() }
            .setNeutralButton("Guardar archivo", null)
            .setNegativeButton("Copiar JSON", null)
            .setCancelable(false)
            .create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Módulo ${m.nombre}", json))
                Toast.makeText(this, "JSON copiado ✓", Toast.LENGTH_SHORT).show()
            }
            dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                guardarArchivo.launch("${m.id}.json")
            }
        }
        dialogo.show()
    }
}
