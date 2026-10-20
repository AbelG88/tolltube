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
    private lateinit var etSerie: EditText
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
        etSerie = findViewById(R.id.etSerie)

        val swActivo = findViewById<android.widget.Switch>(R.id.swActivo)
        swActivo.isChecked = SettingsStore.getActivo(this)
        swActivo.text = if (swActivo.isChecked) "Peaje activado" else "Peaje APAGADO"
        swActivo.setOnCheckedChangeListener { _, activo ->
            SettingsStore.setActivo(this, activo)
            swActivo.text = if (activo) "Peaje activado" else "Peaje APAGADO"
            if (!activo) ExerciseTimerManager.detener()
            PeajeLog.d("AJUSTES interruptor: ${if (activo) "ACTIVADO" else "APAGADO"}")
        }
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
        configurarNivel()
        findViewById<Button>(R.id.btnBorrarHistorial).setOnClickListener {
            SettingsStore.borrarHistorial(this)
            mostrarHistorial()
            Toast.makeText(this, "Progreso borrado", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPin).setOnClickListener { PinGate.configurar(this) }
        configurarDeteccion()

        val swSolo = findViewById<android.widget.Switch>(R.id.swSoloReproduciendo)
        swSolo.isChecked = SettingsStore.getSoloReproduciendo(this)
        swSolo.setOnCheckedChangeListener { _, v ->
            SettingsStore.setSoloReproduciendo(this, v)
            PeajeLog.d("AJUSTES contar solo mientras se reproduce: $v")
        }

        val llAv = findViewById<android.view.View>(R.id.llAvanzado)
        val btnAv = findViewById<Button>(R.id.btnAvanzado)
        btnAv.setOnClickListener {
            val abrir = llAv.visibility != android.view.View.VISIBLE
            llAv.visibility = if (abrir) android.view.View.VISIBLE else android.view.View.GONE
            btnAv.text = if (abrir) "⚙️ Avanzado ▾" else "⚙️ Avanzado ▸"
        }
        // Al arrancar, si el modo normal está activo y falta el permiso de notificaciones, se pide
        if (Deteccion.modo(this) == Deteccion.USO && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        PinGate.exigir(this)
        arrancarMonitorSiCorresponde()
        mostrarHistorial()
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
            if (Deteccion.modo(this@MainActivity) == Deteccion.USO) {
                appendLine("${marca(UsoApps.tienePermiso(this@MainActivity))} Acceso a datos de uso")
                appendLine("${marca(PeajeMonitorService.vivo)} Servicio de monitoreo vivo")
                appendLine("Última app detectada: ${UsoApps.ultimoPaquete ?: "(ninguna todavía)"}")
            } else {
                appendLine("${marca(habilitado)} Accesibilidad activada en Ajustes")
                appendLine("${marca(conectado)} Servicio conectado y vivo")
                appendLine("Última app detectada: $ultimo")
                appendLine("YouTube detectado ahora: ${if (youtube) "sí" else "no"}")
            }
            appendLine("⭐ Ejercicios bien resueltos hoy: ${SettingsStore.aciertosHoy(this@MainActivity)}")
            appendLine("Peaje: ${if (SettingsStore.getActivo(this@MainActivity)) "activado" else "APAGADO"}")
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
        val serie = SettingsStore.getSerie(this)
        etSerie.setText(serie.toString())
        tvIntervalCurrent.text =
            "Cada ${SettingsStore.formatear(total)}: $serie ejercicio${if (serie == 1) "" else "s"} seguido${if (serie == 1) "" else "s"}"
    }

    private fun guardarIntervalo() {
        val minutos = etMinutes.text.toString().trim().toIntOrNull() ?: 0
        val segundos = etSeconds.text.toString().trim().toIntOrNull() ?: 0

        if (segundos > 59) {
            Toast.makeText(this, "Los segundos van de 0 a 59", Toast.LENGTH_SHORT).show()
            return
        }
        val serie = etSerie.text.toString().trim().toIntOrNull() ?: 1
        if (serie < 1 || serie > SettingsStore.MAX_SERIE) {
            Toast.makeText(this, "Los ejercicios seguidos van de 1 a ${SettingsStore.MAX_SERIE}", Toast.LENGTH_SHORT).show()
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
        SettingsStore.setSerie(this, serie)
        ExerciseTimerManager.reprogramar(this)
        PeajeLog.d("AJUSTES intervalo guardado: ${total}s, ejercicios seguidos: $serie")
        mostrarIntervaloActual()
        etMinutes.clearFocus()
        etSeconds.clearFocus()
        Toast.makeText(
            this,
            "Guardado: cada ${SettingsStore.formatear(total)}, $serie seguido(s) ✓",
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
            ExerciseType.EMOCIONES to findViewById(R.id.cbEmociones),
            ExerciseType.LETRAS to findViewById(R.id.cbLetras),
            ExerciseType.COLORES to findViewById(R.id.cbColores),
            ExerciseType.FORMAS to findViewById(R.id.cbFormas),
            ExerciseType.CONTAR to findViewById(R.id.cbContar),
            ExerciseType.SECUENCIAS to findViewById(R.id.cbSecuencias)
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

    // ---------- Modo de detección ----------

    private fun configurarDeteccion() {
        val grupo = findViewById<android.widget.RadioGroup>(R.id.rgDeteccion)
        grupo.check(if (Deteccion.modo(this) == Deteccion.USO) R.id.rbDetUso else R.id.rbDetAcc)
        grupo.setOnCheckedChangeListener { _, id ->
            val modo = if (id == R.id.rbDetUso) Deteccion.USO else Deteccion.ACCESIBILIDAD
            SettingsStore.setDeteccion(this, modo)
            ExerciseTimerManager.detener()
            PeajeLog.d("AJUSTES modo de detección: $modo")
            if (modo == Deteccion.USO) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
                }
                if (!UsoApps.tienePermiso(this)) {
                    Toast.makeText(this, "Falta dar el acceso a datos de uso (botón 3)", Toast.LENGTH_LONG).show()
                }
                arrancarMonitorSiCorresponde()
            } else {
                PeajeMonitorService.detener(this)
            }
        }
        findViewById<Button>(R.id.btnUsoPermiso).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun arrancarMonitorSiCorresponde() {
        if (Deteccion.modo(this) == Deteccion.USO && UsoApps.tienePermiso(this)) {
            PeajeMonitorService.iniciar(this)
        }
    }

    // ---------- Dificultad y progreso ----------

    private fun configurarNivel() {
        val grupo = findViewById<android.widget.RadioGroup>(R.id.rgNivel)
        val ids = listOf(R.id.rbNivel1, R.id.rbNivel2, R.id.rbNivel3)
        grupo.check(ids[SettingsStore.getNivel(this) - 1])
        grupo.setOnCheckedChangeListener { _, id ->
            val nivel = ids.indexOf(id) + 1
            if (nivel > 0) {
                SettingsStore.setNivel(this, nivel)
                PeajeLog.d("AJUSTES dificultad: $nivel")
            }
        }
    }

    private fun mostrarHistorial() {
        val texto = ExerciseType.values().joinToString("\n") { tipo ->
            val (ok, err) = SettingsStore.historial(this, tipo)
            "${tipo.etiqueta}: $ok bien, $err errores"
        }
        findViewById<TextView>(R.id.tvHistorial).text = texto
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
