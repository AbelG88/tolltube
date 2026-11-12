package com.peajecognitivo.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ModulosParseTest {

    private fun invalido(texto: String): String {
        try {
            ModulosStore.parsear(texto)
        } catch (e: IllegalArgumentException) {
            return e.message ?: ""
        }
        fail("Se esperaba un error")
        return ""
    }

    private val minimo = """{"formato":1,"nombre":"Prueba","ejercicios":[
        {"tipo":"numero","pregunta":"¿Cuántos?","correcta":"3"}]}"""

    @Test fun moduloMinimoEsValido() {
        val m = ModulosStore.parsear(minimo)
        assertEquals("Prueba", m.nombre)
        assertEquals("prueba", m.id)
        assertEquals(1, m.items.size)
    }

    @Test fun idSinAcentosNiSimbolos() {
        val m = ModulosStore.parsear(minimo.replace("Prueba", "Días y Meses!"))
        assertEquals("d_as_y_meses", m.id)
    }

    @Test fun jsonRotoSeRechaza() {
        assertTrue(invalido("esto no es json").contains("JSON"))
    }

    @Test fun faltaFormatoSeRechaza() {
        assertTrue(invalido(minimo.replace("\"formato\":1,", "")).contains("formato"))
    }

    @Test fun formatoDistintoSeRechaza() {
        assertTrue(invalido(minimo.replace("\"formato\":1", "\"formato\":2")).contains("formato"))
    }

    @Test fun sinNombreSeRechaza() {
        assertTrue(invalido(minimo.replace("\"nombre\":\"Prueba\",", "")).contains("nombre"))
    }

    @Test fun sinEjerciciosSeRechaza() {
        assertTrue(invalido("""{"formato":1,"nombre":"X","ejercicios":[]}""").contains("no tiene ejercicios"))
    }

    @Test fun numeroConLetrasSeRechaza() {
        assertTrue(invalido(minimo.replace("\"3\"", "\"tres\"")).contains("número"))
    }

    @Test fun opcionesNecesitaDosIncorrectasDistintas() {
        val t = """{"formato":1,"nombre":"X","ejercicios":[
            {"tipo":"opciones","pregunta":"¿?","correcta":"A","incorrectas":["A","B","B"]}]}"""
        assertTrue(invalido(t).contains("al menos 2"))
    }

    @Test fun tipoDesconocidoSeRechaza() {
        assertTrue(invalido(minimo.replace("numero", "dibujo")).contains("tipo"))
    }

    @Test fun textoMuyLargoSeRechaza() {
        assertTrue(invalido(minimo.replace("¿Cuántos?", "x".repeat(201))).contains("largo"))
    }

    /** Todos los módulos que viajan en el APK tienen que ser válidos. */
    @Test fun modulosIncluidosSonValidos() {
        val carpeta = listOf(File("../modulos"), File("modulos")).first { it.isDirectory }
        val archivos = carpeta.listFiles { f -> f.name.endsWith(".json") }!!
        assertTrue("No se encontraron módulos", archivos.isNotEmpty())
        for (f in archivos) {
            try {
                ModulosStore.parsear(f.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                fail("${f.name}: ${e.message}")
            }
        }
    }

    @Test fun ejemploDeLaAppEsValido() {
        assertEquals("Los dinosaurios", ModulosStore.parsear(ModulosStore.EJEMPLO).nombre)
    }
}
