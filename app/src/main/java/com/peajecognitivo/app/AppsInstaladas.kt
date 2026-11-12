package com.peajecognitivo.app

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog

/** Una app instalada que se puede abrir (tiene ícono en el menú). */
data class AppInstalada(val paquete: String, val nombre: String)

/**
 * Lista de apps instaladas. Usa solo las que tienen ícono de lanzador (permiso <queries> del manifiesto),
 * sin el permiso amplio QUERY_ALL_PACKAGES. Todo queda en el celular.
 */
object AppsInstaladas {

    fun listar(context: Context): List<AppInstalada> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            pm.queryIntentActivities(intent, 0)
                .map { AppInstalada(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.paquete != context.packageName }
                .distinctBy { it.paquete }
                .sortedBy { it.nombre.lowercase() }
        } catch (e: Exception) {
            PeajeLog.d("APPS no se pudo listar: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    /** Paquetes de las pantallas de inicio (launchers): nunca piden ejercicio. */
    fun launchers(context: Context): Set<String> = try {
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
    } catch (e: Exception) {
        emptySet()
    }

    /** Nombre legible de un paquete (o el paquete mismo si ya no está instalado). */
    fun nombreDe(context: Context, paquete: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
    } catch (e: Exception) {
        paquete
    }

    /** Las apps de siempre que están instaladas, como paquetes (para empezar a elegir con algo marcado). */
    fun deSiempreInstaladas(context: Context): Set<String> {
        val instalados = listar(context).map { it.paquete }.toSet()
        return AppVigilada.values().flatMap { it.paquetes }.filter { it in instalados }.toSet()
    }

    /** Diálogo con la lista de apps y casillas. Devuelve el conjunto elegido al tocar "Listo". */
    fun elegir(
        context: Context,
        titulo: String,
        marcadas: Set<String>,
        alGuardar: (Set<String>) -> Unit
    ) {
        val apps = listar(context)
        if (apps.isEmpty()) {
            android.widget.Toast.makeText(context, "No pudimos leer la lista de apps", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val estado = BooleanArray(apps.size) { apps[it].paquete in marcadas }
        AlertDialog.Builder(context)
            .setTitle(titulo)
            .setMultiChoiceItems(apps.map { it.nombre }.toTypedArray(), estado) { _, i, v -> estado[i] = v }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Listo") { _, _ ->
                // Lo marcado que ya no figura en la lista (app desinstalada) se conserva
                val visibles = apps.map { it.paquete }.toSet()
                val nuevo = apps.filterIndexed { i, _ -> estado[i] }.map { it.paquete }.toMutableSet()
                nuevo += marcadas.filter { it !in visibles }
                alGuardar(nuevo)
            }
            .show()
    }

    /** "YouTube, Roblox y 2 más" */
    fun resumen(context: Context, paquetes: Collection<String>, vacio: String): String {
        val nombres = paquetes.map { nombreDe(context, it) }.sorted()
        return when {
            nombres.isEmpty() -> vacio
            nombres.size <= 3 -> nombres.joinToString(", ")
            else -> nombres.take(2).joinToString(", ") + " y ${nombres.size - 2} más"
        }
    }
}
