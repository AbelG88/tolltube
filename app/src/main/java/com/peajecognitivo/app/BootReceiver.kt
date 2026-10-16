package com.peajecognitivo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Al prender el celu, vuelve a arrancar el monitoreo si está en modo "Uso de apps". */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Deteccion.modo(context) == Deteccion.USO && UsoApps.tienePermiso(context)) {
            PeajeMonitorService.iniciar(context)
        }
    }
}
