package com.peajecognitivo.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etMinutes: EditText
    private lateinit var etSeconds: EditText
    private lateinit var tvIntervalCurrent: TextView
    private lateinit var tvEstado: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refrescarEstado = object : Runnable {
        override fun run() {
            actualizarEstado()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etMinutes = findViewById(R.id.etMinutes)
        etSeconds = findViewById(R.id.etSeconds)
        tvIntervalCurrent = findViewById(R.id.tvIntervalCurrent)
        tvEstado = findViewById(R.id.tvEstado)

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

        findViewById<Button>(R.id.btnVerRegistro).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        findViewById<TextView>(R.id.tvVersion).text = "Versión ${versionName()}"

        mostrarIntervaloActual()
        configurarTipos()

        val cbVoz = findViewById<CheckBox>(R.id.cbVoz)
        cbVoz.isChecked = SettingsStore.getVoz(this)
        cbVoz.setOnCheckedChangeListener { _, activa ->
            SettingsStore.setVoz(this, activa)
            PeajeLog.d("AJUSTES voz: $activa")
        }
        findViewById<Button>(R.id.btnPin).setOnClickListener { PinGate.configurar(this) }

        PinGate.exigir(this)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refrescarEstado)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refrescarEstado)
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    // ---------- Estado en vivo ----------

    private fun actualizarEstado() {
        val overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val habilitado = servicioHabilitadoEnAjustes()
        val conectado = PeajeAccessibilityService.conectado
        val ultimo = PeajeAccessibilityService.ultimoPaquete.ifEmpty { "(ninguna todavía)" }
        val youtube = PeajeAccessibilityService.isYouTubeActive

        tvEstado.text = buildString {
            appendLine("${marca(overlayOk)} Permiso para dibujar encima de otras apps")
            appendLine("${marca(habilitado)} Accesibilidad activada en Ajustes")
            appendLine("${marca(conectado)} Servicio conectado y vivo")
            appendLine("Última app detectada: $ultimo")
            appendLine("YouTube detectado ahora: ${if (youtube) "sí" else "no"}")
            append("Timer: ${ExerciseTimerManager.descripcionEstado()}")
        }
    }

    private fun marca(ok: Boolean) = if (ok) "✅" else "❌"

    private fun servicioHabilitadoEnAjustes(): Boolean {
        val habilitados = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val nombre = PeajeAccessibilityService::class.java.simpleName
        val completo = "$packageName/${PeajeAccessibilityService::class.java.name}"
        val corto = "$packageName/.$nombre"
        return habilitados.split(':').any {
            it.equals(completo, ignoreCase = true) || it.equals(corto, ignoreCase = true)
        }
    }

    // ---------- Tiempo entre ejercicios ----------

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

    // ---------- Tipos de ejercicio ----------

    private fun configurarTipos() {
        val casillas: Map<ExerciseType, CheckBox> = mapOf(
            ExerciseType.SUMA to findViewById(R.id.cbSuma),
            ExerciseType.RESTA to findViewById(R.id.cbResta),
            ExerciseType.ANIMALES to findViewById(R.id.cbAnimales),
            ExerciseType.OBJETOS to findViewById(R.id.cbObjetos),
            ExerciseType.EMOCIONES to findViewById(R.id.cbEmociones)
        )

        val actuales = SettingsStore.getTipos(this)
        casillas.forEach { (tipo, casilla) -> casilla.isChecked = tipo in actuales }

        casillas.forEach { (_, casilla) ->
            casilla.setOnCheckedChangeListener { _, _ ->
                val elegidos = casillas.filter { it.value.isChecked }.keys
                if (elegidos.isEmpty()) {
                    // Tiene que quedar al menos un tipo: se vuelve a tildar la que se acaba de sacar
                    casilla.isChecked = true
                    Toast.makeText(this, "Tiene que quedar al menos un tipo", Toast.LENGTH_SHORT).show()
                } else {
                    SettingsStore.setTipos(this, elegidos)
                    PeajeLog.d("AJUSTES tipos de ejercicio: ${elegidos.joinToString { it.clave }}")
                }
            }
        }
    }

    // ---------- Permisos ----------

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
