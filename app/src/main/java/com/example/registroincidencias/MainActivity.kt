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
 * Permite escribir el titulo y la descripcion de una incidencia, asignarle
 * una prioridad y registrarla en una lista en memoria. Al registrar, la
 * pantalla reacciona de inmediato mostrando un mensaje de retroalimentacion
 * ("Reporte preparado: <titulo>"), ademas de actualizar el contador y el
 * listado con el nuevo estado.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Lista en memoria con las incidencias registradas en la sesion actual. */
    private val incidencias = mutableListOf<Incidencia>()

    /** Formato de hora usado para marcar cada incidencia. */
    private val formatoHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Modelo simple de datos de una incidencia. */
    data class Incidencia(
        val titulo: String,
        val descripcion: String,
        val prioridad: String,
        val marcaTiempo: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Estado inicial del mensaje de retroalimentacion.
        binding.txtMensaje.text = getString(R.string.mensaje_inicial)

        actualizarPantalla()

        binding.btnRegistrar.setOnClickListener { registrarIncidencia() }
        binding.btnLimpiar.setOnClickListener { limpiarRegistro() }
    }

    /** Valida el formulario, agrega la incidencia a la lista y actualiza el mensaje de estado. */
    private fun registrarIncidencia() {
        val titulo = binding.edtTitulo.text?.toString()?.trim().orEmpty()
        val descripcion = binding.edtDescripcion.text?.toString()?.trim().orEmpty()

        if (titulo.isEmpty()) {
            binding.inputLayoutTitulo.error = getString(R.string.error_titulo_vacio)
            return
        }
        binding.inputLayoutTitulo.error = null

        if (descripcion.isEmpty()) {
            binding.inputLayoutDescripcion.error = getString(R.string.error_vacio)
            return
        }
        binding.inputLayoutDescripcion.error = null

        val prioridad = binding.spPrioridad.selectedItem.toString()
        val marcaTiempo = formatoHora.format(Date())

        incidencias.add(Incidencia(titulo, descripcion, prioridad, marcaTiempo))

        // Retroalimentacion visible inmediata a partir del estado capturado.
        binding.txtMensaje.text = getString(R.string.reporte_preparado, titulo)

        binding.edtTitulo.setText("")
        binding.edtDescripcion.setText("")
        actualizarPantalla()

        Snackbar.make(binding.root, R.string.msg_registrada, Snackbar.LENGTH_SHORT).show()
    }

    /** Borra todas las incidencias registradas y reinicia el contador y el mensaje. */
    private fun limpiarRegistro() {
        incidencias.clear()
        binding.edtTitulo.setText("")
        binding.edtDescripcion.setText("")
        binding.inputLayoutTitulo.error = null
        binding.inputLayoutDescripcion.error = null
        binding.txtMensaje.text = getString(R.string.mensaje_inicial)
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
            val etiqueta = "${indice + 1}. [${incidencia.prioridad}] ${incidencia.titulo}\n"
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

            texto.append("     ${incidencia.descripcion}\n")
            texto.append("     Registrada: ${incidencia.marcaTiempo}\n\n")
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
