package com.peajecognitivo.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etMinutes: EditText
    private lateinit var etSeconds: EditText
    private lateinit var tvIntervalCurrent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etMinutes = findViewById(R.id.etMinutes)
        etSeconds = findViewById(R.id.etSeconds)
        tvIntervalCurrent = findViewById(R.id.tvIntervalCurrent)

        findViewById<Button>(R.id.btnEnableOverlay).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<Button>(R.id.btnEnableAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }

        findViewById<Button>(R.id.btnTestOverlay).setOnClickListener {
            PeajeOverlayManager.show(this)
        }

        findViewById<Button>(R.id.btnSaveInterval).setOnClickListener {
            guardarIntervalo()
        }

        findViewById<TextView>(R.id.tvVersion).text = "Versión ${versionName()}"

        mostrarIntervaloActual()
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    private fun mostrarIntervaloActual() {
        val total = SettingsStore.getIntervalSeconds(this)
        etMinutes.setText((total / 60).toString())
        etSeconds.setText((total % 60).toString())
        tvIntervalCurrent.text = "Tiempo actual: ${SettingsStore.formatear(total)}"
    }

    private fun guardarIntervalo() {
        val minutos = etMinutes.text.toString().trim().toIntOrNull() ?: 0
        val segundos = etSeconds.text.toString().trim().toIntOrNull() ?: 0

        if (segundos > 59) {
            Toast.makeText(this, "Los segundos van de 0 a 59", Toast.LENGTH_SHORT).show()
            return
        }
        val total = minutos * 60 + segundos
        if (total < SettingsStore.MIN_SECONDS) {
            Toast.makeText(
                this,
                "El mínimo es ${SettingsStore.MIN_SECONDS} segundos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (total > SettingsStore.MAX_SECONDS) {
            Toast.makeText(this, "El máximo es 3 horas", Toast.LENGTH_SHORT).show()
            return
        }

        SettingsStore.setIntervalSeconds(this, total)
        ExerciseTimerManager.reprogramar(this)
        PeajeLog.d("AJUSTES intervalo guardado: ${total}s")
        mostrarIntervaloActual()
        etMinutes.clearFocus()
        etSeconds.clearFocus()
        Toast.makeText(
            this,
            "Guardado: cada ${SettingsStore.formatear(total)} ✓",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permiso de overlay ya concedido ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
