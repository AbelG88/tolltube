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
import android.widget.CompoundButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var ninoCargado = ""
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
        if (!SettingsStore.onboardingHecho(this)) {
            // Primera vez: el asistente configura todo paso a paso
            startActivity(Intent(this, AsistenteActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        ninoCargado = Ninos.activo(this)
        armarNinos()

        findViewById<LinearLayout>(R.id.llApoyo).addView(Donaciones.botones(this))
        val llAvanzado = findViewById<LinearLayout>(R.id.llAvanzado)
        val btnMas = findViewById<Button>(R.id.btnMasAjustes)
        btnMas.setOnClickListener {
            val abrir = llAvanzado.visibility != android.view.View.VISIBLE
            llAvanzado.visibility = if (abrir) android.view.View.VISIBLE else android.view.View.GONE
            btnMas.text = if (abrir) "Ocultar avanzado" else "Avanzado"
        }
        findViewById<Button>(R.id.btnAsistente).setOnClickListener {
            startActivity(Intent(this, AsistenteActivity::class.java))
        }
        configurarNavegacion()
        configurarModoCuidado()
        findViewById<Button>(R.id.btnApps).setOnClickListener { elegirApps() }
        findViewById<Button>(R.id.btnCambiarApps).setOnClickListener { elegirApps() }

        etMinutes = findViewById(R.id.etMinutes)
        etSeconds = findViewById(R.id.etSeconds)
        etSerie = findViewById(R.id.etSerie)

        val swActivo = findViewById<CompoundButton>(R.id.swActivo)
        swActivo.isChecked = SettingsStore.getActivo(this)
        swActivo.text = if (swActivo.isChecked) "Peaje activado" else "Peaje APAGADO"
        swActivo.setOnCheckedChangeListener { _, activo ->
            SettingsStore.setActivo(this, activo)
            swActivo.text = if (activo) "Peaje activado" else "Peaje APAGADO"
            if (!activo) {
                ExerciseTimerManager.detener()
                PeajeMonitorService.detener(this) // sin servicio no hay notificación
            } else {
                arrancarMonitorSiCorresponde()
            }
            PeajeLog.d("AJUSTES interruptor: ${if (activo) "ACTIVADO" else "APAGADO"}")
        }
        tvIntervalCurrent = findViewById(R.id.tvIntervalCurrent)
        tvEstado = findViewById(R.id.tvEstado)

        findViewById<Button>(R.id.btnEnableOverlay).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<Button>(R.id.btnTestOverlay).setOnClickListener {
            PeajeOverlayManager.show(this)
        }

        findViewById<Button>(R.id.btnSaveInterval).setOnClickListener {
            guardarIntervalo()
        }

        if (!BuildConfig.LOG_DIAGNOSTICO) findViewById<Button>(R.id.btnVerRegistro).visibility = android.view.View.GONE
        findViewById<Button>(R.id.btnVerRegistro).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        findViewById<TextView>(R.id.tvVersion).text = "Versión ${versionName()}"

        mostrarIntervaloActual()
        configurarTipos()

        val cbVoz = findViewById<CompoundButton>(R.id.cbVoz)
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
        findViewById<Button>(R.id.btnModulos).setOnClickListener {
            startActivity(Intent(this, ModulosActivity::class.java))
        }

        val swSolo = findViewById<CompoundButton>(R.id.swSoloReproduciendo)
        swSolo.isChecked = SettingsStore.getSoloReproduciendo(this)
        swSolo.setOnCheckedChangeListener { _, v ->
            SettingsStore.setSoloReproduciendo(this, v)
            PeajeLog.d("AJUSTES contar solo mientras se reproduce: $v")
        }

        // Al arrancar, si el modo normal está activo y falta el permiso de notificaciones, se pide
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Ninos.activo(this) != ninoCargado) { // el asistente agregó o cambió de niño
            recreate()
            return
        }
        PinGate.exigir(this)
        PinGate.exigirCrear(this)
        arrancarMonitorSiCorresponde()
        mostrarHistorial()
        refrescarModulos()
        mostrarResumen()
        Donaciones.mostrarSiToca(this)
        handler.post(refrescarEstado)
    }

    override fun onPause() {
        super.onPause()
        Donaciones.cancelarPendiente()
        handler.removeCallbacks(refrescarEstado)
    }

    /** Una ficha por niño (elige el activo) y "+ Agregar" al final. */
    private fun armarNinos() {
        val grupo = findViewById<ChipGroup>(R.id.cgNinos)
        grupo.removeAllViews()
        val activo = Ninos.activo(this)
        Ninos.ids(this).forEach { id ->
            val chip = Chip(this).apply {
                this.id = android.view.View.generateViewId()
                text = Ninos.nombreDe(this@MainActivity, id)
                isCheckable = true
                isChecked = id == activo
                setOnClickListener {
                    if (id != Ninos.activo(this@MainActivity)) {
                        Ninos.setActivo(this@MainActivity, id)
                        ExerciseTimerManager.reprogramar(this@MainActivity)
                        PeajeLog.d("NINOS cambio de niño activo")
                        recreate()
                    }
                }
            }
            grupo.addView(chip)
        }
        if (Ninos.ids(this).size < Ninos.MAX) {
            grupo.addView(Chip(this).apply {
                text = "+ Agregar"
                isCheckable = false
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, AsistenteActivity::class.java)
                        .putExtra(AsistenteActivity.EXTRA_NUEVO, true))
                }
            })
        }
        val btnQuitar = findViewById<Button>(R.id.btnQuitarNino)
        btnQuitar.visibility = if (Ninos.ids(this).size > 1) android.view.View.VISIBLE else android.view.View.GONE
        btnQuitar.setOnClickListener {
            val nombre = Ninos.nombreDe(this, Ninos.activo(this))
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("¿Quitar a $nombre?")
                .setMessage("Se borran sus ajustes y su progreso. No se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Quitar") { _, _ ->
                    Ninos.quitar(this, Ninos.activo(this))
                    ExerciseTimerManager.reprogramar(this)
                    recreate()
                }
                .show()
        }
    }

    private fun mostrarResumen() {
        val niveles = listOf("Fácil", "Medio", "Difícil")
        val nombre = SettingsStore.getNombreNino(this)
        findViewById<TextView>(R.id.tvNombre).text = nombre.ifEmpty { Ninos.nombreDe(this, Ninos.activo(this)) }
        findViewById<TextView>(R.id.tvHoy).text = SettingsStore.aciertosHoy(this).toString()
        val seg = SettingsStore.getIntervalSeconds(this)
        findViewById<TextView>(R.id.tvCada).text =
            if (seg % 60 == 0) "${seg / 60} min" else SettingsStore.formatear(seg)
        findViewById<TextView>(R.id.tvCadaEtiqueta).text = "${SettingsStore.getSerie(this)} por vez"
        val edad = SettingsStore.getEdad(this)
        val modo = SettingsStore.getModoCuidado(this)
        findViewById<TextView>(R.id.tvResumen).text =
            "Modo ${modo.etiqueta}" + if (edad > 0) " · $edad años" else ""
        findViewById<TextView>(R.id.tvCuidamos).text = resumenApps()
        findViewById<Button>(R.id.btnApps).text = "Apps que cuidamos: " + resumenApps()
    }

    /** Qué apps piden ejercicio, en una frase corta. */
    private fun resumenApps(): String = when (SettingsStore.getModoApps(this)) {
        ModoApps.SIEMPRE -> AppVigilada.values().filter { SettingsStore.appVigilada(this, it) }
            .joinToString(", ") { it.nombre }.ifEmpty { "ninguna" }
        ModoApps.ELEGIR -> AppsInstaladas.resumen(this, SettingsStore.getAppsElegidas(this), "ninguna")
        ModoApps.TODO -> "Todo, menos " + AppsInstaladas.resumen(this, SettingsStore.getAppsLibres(this), "nada")
    }

    /** Elegir apps desde el panel: primero el modo, después (si hace falta) la lista. */
    private fun elegirApps() {
        val modos = arrayOf("Las de siempre", "Elijo yo cuáles", "Todo menos lo que permito")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Qué apps cuidamos?")
            .setItems(modos) { _, i ->
                val modo = ModoApps.values()[i]
                if (modo == ModoApps.SIEMPRE) {
                    SettingsStore.setModoApps(this, modo)
                    AppVigilada.values().forEach { SettingsStore.setAppVigilada(this, it, true) }
                    mostrarResumen()
                } else {
                    cambiarModoApps(modo)
                }
            }
            .show()
    }

    private fun cambiarModoApps(modo: ModoApps) {
        if (modo == ModoApps.ELEGIR) {
            val previas = SettingsStore.getAppsElegidas(this).ifEmpty { AppsInstaladas.deSiempreInstaladas(this) }
            AppsInstaladas.elegir(this, "Apps que cuidamos", previas) { elegidas ->
                if (elegidas.isEmpty()) {
                    Toast.makeText(this, "Tiene que quedar al menos una app", Toast.LENGTH_SHORT).show()
                } else {
                    SettingsStore.setModoApps(this, ModoApps.ELEGIR)
                    SettingsStore.setAppsElegidas(this, elegidas)
                    PeajeLog.d("AJUSTES apps: modo elegir, ${elegidas.size} apps")
                    mostrarResumen()
                }
            }
        } else {
            AppsInstaladas.elegir(this, "Apps libres (sin ejercicio)", SettingsStore.getAppsLibres(this)) { libres ->
                SettingsStore.setModoApps(this, ModoApps.TODO)
                SettingsStore.setAppsLibres(this, libres)
                PeajeLog.d("AJUSTES apps: modo todo, ${libres.size} libres")
                mostrarResumen()
            }
        }
    }

    // ---------- Navegación (Inicio / Progreso / Ajustes) ----------

    private fun configurarNavegacion() {
        val navs = listOf(
            R.id.navInicio to R.id.secInicio,
            R.id.navProgreso to R.id.secProgreso,
            R.id.navAjustes to R.id.secAjustes
        )
        fun ir(seleccion: Int) {
            navs.forEach { (nav, sec) ->
                val activa = nav == seleccion
                findViewById<android.view.View>(sec).visibility = if (activa) android.view.View.VISIBLE else android.view.View.GONE
                findViewById<TextView>(nav).setTextColor(getColor(if (activa) R.color.accent else R.color.text_secondary))
            }
        }
        navs.forEach { (nav, _) -> findViewById<android.view.View>(nav).setOnClickListener { ir(nav) } }
        ir(R.id.navInicio)
    }

    private fun configurarModoCuidado() {
        val ids = mapOf(
            ModoCuidado.SUAVE to R.id.chSuave,
            ModoCuidado.NORMAL to R.id.chNormal,
            ModoCuidado.INTENSO to R.id.chIntenso
        )
        val grupo = findViewById<ChipGroup>(R.id.cgModoCuidado)
        val detalle = findViewById<TextView>(R.id.tvModoDetalle)
        val actual = SettingsStore.getModoCuidado(this)
        grupo.check(ids.getValue(actual))
        detalle.text = actual.detalle
        grupo.setOnCheckedStateChangeListener { _, checked ->
            val modo = ids.entries.firstOrNull { it.value == checked.firstOrNull() }?.key ?: return@setOnCheckedStateChangeListener
            SettingsStore.setModoCuidado(this, modo)
            ExerciseTimerManager.reprogramar(this)
            detalle.text = modo.detalle
            mostrarIntervaloActual()
            mostrarResumen()
            PeajeLog.d("AJUSTES modo de cuidado: ${modo.clave}")
        }
    }

    /** Un solo aviso: o todo funciona, o falta algo y hay un botón para arreglarlo. */
    private fun actualizarBanner() {
        val overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val usoOk = UsoApps.tienePermiso(this)
        val nombre = SettingsStore.getNombreNino(this).ifEmpty { "el peque" }
        val titulo = findViewById<TextView>(R.id.tvBannerTitulo)
        val detalle = findViewById<TextView>(R.id.tvBannerDetalle)
        val boton = findViewById<Button>(R.id.btnArreglar)
        val fondo = findViewById<android.view.View>(R.id.cardBanner).background.mutate() as android.graphics.drawable.GradientDrawable?
        fun pintar(color: Int) { fondo?.setColor(getColor(color)) }
        when {
            !SettingsStore.getActivo(this) -> {
                pintar(R.color.pastel_butter)
                titulo.text = "Peaje apagado"
                detalle.text = "Por ahora no se está cuidando nada. Se prende en Ajustes."
                boton.visibility = android.view.View.GONE
            }
            !usoOk || !overlayOk -> {
                pintar(R.color.pastel_butter)
                titulo.text = "Falta un permiso"
                detalle.text = "Por ahora no se está cuidando nada."
                boton.visibility = android.view.View.VISIBLE
                boton.setOnClickListener {
                    if (!usoOk) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
            else -> {
                pintar(R.color.pastel_mint)
                titulo.text = "Todo funcionando"
                detalle.text = "$nombre puede usar el celu tranquilo."
                boton.visibility = android.view.View.GONE
            }
        }
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    // ---------- Estado en vivo ----------

    private fun actualizarEstado() {
        actualizarBanner()
        val overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

        tvEstado.text = buildString {
            appendLine("${marca(overlayOk)} Permiso para dibujar encima de otras apps")
            appendLine("${marca(UsoApps.tienePermiso(this@MainActivity))} Acceso a datos de uso")
            appendLine("${marca(PeajeMonitorService.vivo)} Servicio de monitoreo vivo")
            appendLine("Última app detectada: ${UsoApps.ultimoPaquete ?: "(ninguna todavía)"}")
            appendLine("★ Ejercicios bien resueltos hoy: ${SettingsStore.aciertosHoy(this@MainActivity)}")
            appendLine("Peaje: ${if (SettingsStore.getActivo(this@MainActivity)) "activado" else "APAGADO"}")
            append("Timer: ${ExerciseTimerManager.descripcionEstado()}")
        }
    }

    private fun marca(ok: Boolean) = if (ok) "✓" else "✕"

    // ---------- Tiempo entre ejercicios ----------

    private fun mostrarIntervaloActual() {
        val total = SettingsStore.getIntervalSeconds(this)
        etMinutes.setText((total / 60).toString())
        etSeconds.setText((total % 60).toString())
        val serie = SettingsStore.getSerie(this)
        etSerie.setText(serie.toString())
        tvIntervalCurrent.text =
            "Cada ${SettingsStore.formatear(total)} · $serie ejercicio${if (serie == 1) "" else "s"} seguido${if (serie == 1) "" else "s"}"
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
        val casillas: Map<ExerciseType, CompoundButton> = mapOf(
            ExerciseType.SUMA to findViewById(R.id.cbSuma),
            ExerciseType.RESTA to findViewById(R.id.cbResta),
            ExerciseType.CONTAR to findViewById(R.id.cbContar)
        )

        val actuales = SettingsStore.getTipos(this)
        casillas.forEach { (tipo, casilla) -> casilla.isChecked = tipo in actuales }

        casillas.forEach { (_, casilla) ->
            casilla.setOnCheckedChangeListener { _, _ ->
                val elegidos = casillas.filter { it.value.isChecked }.keys
                if (elegidos.isEmpty() && !ModulosStore.hayActivos(this)) {
                    // Tiene que quedar al menos una opción (de fábrica o módulo)
                    casilla.isChecked = true
                    Toast.makeText(this, "Tiene que quedar al menos un tipo o módulo", Toast.LENGTH_SHORT).show()
                } else {
                    SettingsStore.setTipos(this, elegidos)
                    PeajeLog.d("AJUSTES tipos de ejercicio: ${elegidos.joinToString { it.clave }}")
                }
            }
        }
        refrescarModulos()
    }

    private fun fabricaTildados(): Int =
        listOf(R.id.cbSuma, R.id.cbResta, R.id.cbContar).count { findViewById<CompoundButton>(it).isChecked }

    /** Cada módulo importado es un chip (se activa/desactiva tocándolo) con una X para quitarlo. */
    private fun refrescarModulos() {
        val grupo = findViewById<ChipGroup>(R.id.cgModulos)
        grupo.removeAllViews()
        ModulosStore.listar(this).forEach { m ->
            val chip = Chip(this).apply {
                text = m.nombre + (if (m.edad.isNotEmpty()) " · ${m.edad}" else "")
                isCheckable = true
                isChecked = ModulosStore.activo(this@MainActivity, m.id)
                isCloseIconVisible = true
                setOnCheckedChangeListener { _, v ->
                    val otrosActivos = ModulosStore.listar(this@MainActivity)
                        .any { it.id != m.id && ModulosStore.activo(this@MainActivity, it.id) }
                    if (!v && fabricaTildados() == 0 && !otrosActivos) {
                        isChecked = true
                        Toast.makeText(this@MainActivity, "Tiene que quedar al menos un tipo o módulo", Toast.LENGTH_SHORT).show()
                    } else {
                        ModulosStore.setActivo(this@MainActivity, m.id, v)
                        PeajeLog.d("AJUSTES módulo '${m.nombre}': $v")
                    }
                }
                setOnCloseIconClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("¿Quitar el módulo?")
                        .setMessage(m.nombre)
                        .setPositiveButton("Quitar") { _, _ ->
                            ModulosStore.borrar(this@MainActivity, m.id)
                            if (fabricaTildados() == 0 && !ModulosStore.hayActivos(this@MainActivity)) {
                                // No puede quedar sin ejercicios: se vuelven a activar los de fábrica
                                listOf(R.id.cbSuma, R.id.cbResta, R.id.cbContar)
                                    .forEach { findViewById<CompoundButton>(it).isChecked = true }
                            }
                            refrescarModulos()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            grupo.addView(chip)
        }
    }

    // ---------- Modo de detección ----------

    private fun configurarDeteccion() {
        findViewById<Button>(R.id.btnUsoPermiso).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun arrancarMonitorSiCorresponde() {
        if (SettingsStore.getActivo(this) && UsoApps.tienePermiso(this)) {
            PeajeMonitorService.iniciar(this)
        }
    }

    // ---------- Dificultad y progreso ----------

    private fun configurarNivel() {
        val grupo = findViewById<ChipGroup>(R.id.cgNivel)
        val ids = listOf(R.id.chNivel1, R.id.chNivel2, R.id.chNivel3)
        grupo.check(ids[SettingsStore.getNivel(this) - 1])
        grupo.setOnCheckedStateChangeListener { _, checkedIds ->
            val nivel = ids.indexOf(checkedIds.firstOrNull()) + 1
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
}
