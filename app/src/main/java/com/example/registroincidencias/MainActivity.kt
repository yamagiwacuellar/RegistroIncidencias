package com.example.registroincidencias

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.example.registroincidencias.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal de la aplicacion "Registro de Incidencias".
 *
 * Permite escribir la descripcion de una incidencia, asignarle una prioridad
 * y registrarla en una lista en memoria. Muestra ademas un contador con el
 * total de incidencias registradas durante la sesion.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Lista en memoria con las incidencias registradas en la sesion actual. */
    private val incidencias = mutableListOf<Incidencia>()

    /** Formato de hora usado para marcar cada incidencia. */
    private val formatoHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Modelo simple de datos de una incidencia. */
    data class Incidencia(
        val descripcion: String,
        val prioridad: String,
        val marcaTiempo: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actualizarPantalla()

        binding.btnRegistrar.setOnClickListener { registrarIncidencia() }
        binding.btnLimpiar.setOnClickListener { limpiarRegistro() }
    }

    /** Valida el formulario y agrega la incidencia a la lista. */
    private fun registrarIncidencia() {
        val descripcion = binding.edtDescripcion.text?.toString()?.trim().orEmpty()

        if (descripcion.isEmpty()) {
            binding.inputLayoutDescripcion.error = getString(R.string.error_vacio)
            return
        }

        binding.inputLayoutDescripcion.error = null

        val prioridad = binding.spPrioridad.selectedItem.toString()
        val marcaTiempo = formatoHora.format(Date())

        incidencias.add(Incidencia(descripcion, prioridad, marcaTiempo))

        binding.edtDescripcion.setText("")
        actualizarPantalla()

        Snackbar.make(binding.root, R.string.msg_registrada, Snackbar.LENGTH_SHORT).show()
    }

    /** Borra todas las incidencias registradas y reinicia el contador. */
    private fun limpiarRegistro() {
        incidencias.clear()
        binding.edtDescripcion.setText("")
        binding.inputLayoutDescripcion.error = null
        actualizarPantalla()

        Snackbar.make(binding.root, R.string.msg_limpiado, Snackbar.LENGTH_SHORT).show()
    }

    /** Refresca el contador y el listado que se muestran en pantalla. */
    private fun actualizarPantalla() {
        binding.txtContador.text = getString(R.string.contador_formato, incidencias.size)

        if (incidencias.isEmpty()) {
            binding.txtListado.text = getString(R.string.sin_registros)
            return
        }

        val texto = SpannableStringBuilder()

        incidencias.forEachIndexed { indice, incidencia ->
            val inicio = texto.length
            val etiqueta = "${indice + 1}. [${incidencia.prioridad}] "
            texto.append(etiqueta)

            texto.setSpan(
                ForegroundColorSpan(colorDePrioridad(incidencia.prioridad)),
                inicio,
                inicio + etiqueta.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            texto.setSpan(
                StyleSpan(Typeface.BOLD),
                inicio,
                inicio + etiqueta.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            texto.append(incidencia.descripcion)
            texto.append("\n     Registrada: ${incidencia.marcaTiempo}\n\n")
        }

        binding.txtListado.text = texto
    }

    /** Devuelve el color asociado a cada nivel de prioridad. */
    private fun colorDePrioridad(prioridad: String): Int {
        val recurso = when (prioridad.lowercase(Locale.getDefault())) {
            "alta" -> R.color.prioridad_alta
            "media" -> R.color.prioridad_media
            else -> R.color.prioridad_baja
        }
        return ContextCompat.getColor(this, recurso)
    }
}
