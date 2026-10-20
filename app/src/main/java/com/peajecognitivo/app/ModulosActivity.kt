package com.peajecognitivo.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Pantalla para importar, activar y quitar módulos de ejercicios (archivos JSON). */
class ModulosActivity : AppCompatActivity() {

    private lateinit var lista: LinearLayout

    private val selector =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) importar(uri)
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Módulos de ejercicios"

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }

        raiz.addView(TextView(this).apply {
            text = "📦 Módulos de ejercicios"
            textSize = 24f
            setTextColor(0xFF333333.toInt())
        })
        raiz.addView(TextView(this).apply {
            text = "Paquetes de ejercicios temáticos o por edad, en archivos .json. Se mezclan con los de fábrica o los reemplazan."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        })

        val swFabrica = Switch(this).apply {
            text = "Usar también los ejercicios de fábrica"
            textSize = 16f
            isChecked = SettingsStore.getFabrica(this@ModulosActivity)
            setOnCheckedChangeListener { _, v ->
                SettingsStore.setFabrica(this@ModulosActivity, v)
                PeajeLog.d("AJUSTES ejercicios de fábrica: $v")
                if (!v && !ModulosStore.hayActivos(this@ModulosActivity)) {
                    Toast.makeText(
                        this@ModulosActivity,
                        "Sin módulos activos se usan los de fábrica igual",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        raiz.addView(swFabrica)

        raiz.addView(Button(this).apply {
            text = "📥 Importar módulo (.json)"
            setOnClickListener { selector.launch(arrayOf("*/*")) }
        })

        raiz.addView(TextView(this).apply {
            text = "Módulos cargados"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(20), 0, dp(4))
        })

        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        raiz.addView(lista)

        raiz.addView(TextView(this).apply {
            text = "¿Cómo armar un módulo?"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(24), 0, dp(4))
        })
        raiz.addView(TextView(this).apply {
            text = "Tocá \"Copiar modelo\", pegalo donde lo vayas a editar (o pasáselo a una IA pidiéndole un módulo sobre el tema y la edad que quieras) y guardalo con extensión .json. Tipos: \"opciones\" (3 botones: la correcta y 2 incorrectas) y \"numero\" (se responde con el teclado)."
            textSize = 14f
        })
        raiz.addView(Button(this).apply {
            text = "📋 Copiar modelo"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Modelo de módulo", ModulosStore.EJEMPLO))
                Toast.makeText(this@ModulosActivity, "Modelo copiado ✓", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
            addView(raiz)
        })
        refrescar()
    }

    override fun onResume() {
        super.onResume()
        PinGate.exigir(this)
    }

    private fun importar(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("No se pudo abrir el archivo")
            if (bytes.size > ModulosStore.MAX_BYTES) {
                throw IllegalArgumentException("El archivo es muy grande (máximo ${ModulosStore.MAX_BYTES / 1024} KB)")
            }
            val m = ModulosStore.importar(this, String(bytes, Charsets.UTF_8))
            Toast.makeText(this, "Módulo \"${m.nombre}\" importado (${m.items.size} ejercicios) ✓", Toast.LENGTH_LONG).show()
            refrescar()
        } catch (e: IllegalArgumentException) {
            PeajeLog.d("MODULOS importación rechazada: ${e.message}")
            AlertDialog.Builder(this)
                .setTitle("No se pudo importar")
                .setMessage(e.message)
                .setPositiveButton("Entendido", null)
                .show()
        } catch (e: Exception) {
            PeajeLog.d("MODULOS error al importar: ${e.javaClass.simpleName}: ${e.message}")
            Toast.makeText(this, "No se pudo leer el archivo", Toast.LENGTH_LONG).show()
        }
    }

    private fun refrescar() {
        lista.removeAllViews()
        val modulos = ModulosStore.listar(this)
        if (modulos.isEmpty()) {
            lista.addView(TextView(this).apply {
                text = "Todavía no hay módulos importados."
                textSize = 14f
            })
            return
        }
        modulos.forEach { m ->
            val detalle = listOf(m.edad, "${m.items.size} ejercicios").filter { it.isNotEmpty() }.joinToString(" · ")
            lista.addView(Switch(this).apply {
                text = "${m.nombre}\n$detalle"
                textSize = 16f
                isChecked = ModulosStore.activo(this@ModulosActivity, m.id)
                setOnCheckedChangeListener { _, v ->
                    ModulosStore.setActivo(this@ModulosActivity, m.id, v)
                    PeajeLog.d("AJUSTES módulo '${m.nombre}': $v")
                }
            })
            lista.addView(Button(this).apply {
                text = "Quitar \"${m.nombre}\""
                visibility = View.VISIBLE
                setOnClickListener {
                    AlertDialog.Builder(this@ModulosActivity)
                        .setTitle("¿Quitar el módulo?")
                        .setMessage(m.nombre)
                        .setPositiveButton("Quitar") { _, _ ->
                            ModulosStore.borrar(this@ModulosActivity, m.id)
                            refrescar()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            })
        }
    }
}
