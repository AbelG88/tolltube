package com.peajecognitivo.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Muestra el registro de diagnóstico para poder copiarlo o compartirlo desde el celular. */
class LogActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        title = "Registro de Peaje Cognitivo"

        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        findViewById<Button>(R.id.btnCopiar).setOnClickListener { copiar() }
        findViewById<Button>(R.id.btnCompartir).setOnClickListener { compartir() }
        findViewById<Button>(R.id.btnActualizar).setOnClickListener { cargar() }
        findViewById<Button>(R.id.btnBorrar).setOnClickListener {
            PeajeLog.borrar(this)
            cargar()
        }

        cargar()
    }

    private fun textoLog(): String {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        return "Versión de la app: $version\n" + PeajeLog.leer(this, LINEAS)
    }

    override fun onResume() {
        super.onResume()
        PinGate.exigir(this)
    }

    private fun cargar() {
        tvLog.text = textoLog()
        scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun copiar() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Registro Peaje", textoLog()))
        Toast.makeText(this, "Registro copiado ✓", Toast.LENGTH_SHORT).show()
    }

    private fun compartir() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Registro Peaje Cognitivo")
            putExtra(Intent.EXTRA_TEXT, textoLog())
        }
        startActivity(Intent.createChooser(intent, "Compartir registro"))
    }

    companion object {
        private const val LINEAS = 300
    }
}
