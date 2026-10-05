package com.example.registroincidencias

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.example.registroincidencias.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal de la aplicacion "Registro de Incidencias".
 *
 * Permite escribir el titulo y la descripcion de una incidencia (con un
 * teclado contextual: capitalizacion automatica y acciones IME "Siguiente"
 * / "Listo"), elegir la prioridad mediante una interaccion tactil de
 * tarjetas seleccionables, y registrarla en una lista en memoria. Al
 * registrar, la pantalla reacciona de inmediato mostrando un mensaje de
 * retroalimentacion ("Reporte preparado: <titulo>"), ademas de actualizar
 * el contador y el listado con el nuevo estado.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Lista en memoria con las incidencias registradas en la sesion actual. */
    private val incidencias = mutableListOf<Incidencia>()

    /** Formato de hora usado para marcar cada incidencia. */
    private val formatoHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Prioridad elegida mediante la interaccion tactil (tarjetas), o null si aun no se elige. */
    private var prioridadSeleccionada: String? = null

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
        binding.txtPrioridadSeleccionada.text = getString(R.string.prioridad_no_seleccionada)
        actualizarTarjetasPrioridad()

        actualizarPantalla()

        binding.btnRegistrar.setOnClickListener { registrarIncidencia() }
        binding.btnLimpiar.setOnClickListener { limpiarRegistro() }

        // Interaccion tactil adicional: elegir prioridad tocando una tarjeta.
        binding.cardPrioridadBaja.setOnClickListener { seleccionarPrioridad(getString(R.string.prioridad_baja_label)) }
        binding.cardPrioridadMedia.setOnClickListener { seleccionarPrioridad(getString(R.string.prioridad_media_label)) }
        binding.cardPrioridadAlta.setOnClickListener { seleccionarPrioridad(getString(R.string.prioridad_alta_label)) }

        // Accion IME "Listo" del campo de descripcion dispara el registro.
        binding.edtDescripcion.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registrarIncidencia()
                true
            } else {
                false
            }
        }
    }

    /** Actualiza el estado de prioridad elegido y su retroalimentacion visible. */
    private fun seleccionarPrioridad(prioridad: String) {
        prioridadSeleccionada = prioridad
        binding.txtPrioridadSeleccionada.text = getString(R.string.prioridad_seleccionada_formato, prioridad)
        binding.txtPrioridadSeleccionada.setTextColor(colorDePrioridad(prioridad))
        actualizarTarjetasPrioridad()
    }

    /** Resalta la tarjeta de prioridad actualmente seleccionada y atenua las demas. */
    private fun actualizarTarjetasPrioridad() {
        val grosorBorde = (2 * resources.displayMetrics.density).toInt()
        val colorInactivo = ContextCompat.getColor(this, R.color.gris_texto)
        val blanco = ContextCompat.getColor(this, R.color.blanco)

        val tarjetas = listOf(
            Triple(getString(R.string.prioridad_baja_label), binding.cardPrioridadBaja, binding.txtCardBaja),
            Triple(getString(R.string.prioridad_media_label), binding.cardPrioridadMedia, binding.txtCardMedia),
            Triple(getString(R.string.prioridad_alta_label), binding.cardPrioridadAlta, binding.txtCardAlta)
        )

        tarjetas.forEach { (nombre, tarjeta: MaterialCardView, etiqueta) ->
            tarjeta.strokeWidth = grosorBorde
            if (nombre == prioridadSeleccionada) {
                val color = colorDePrioridad(nombre)
                tarjeta.setStrokeColor(color)
                tarjeta.setCardBackgroundColor(color)
                etiqueta.setTextColor(blanco)
            } else {
                tarjeta.setStrokeColor(colorInactivo)
                tarjeta.setCardBackgroundColor(blanco)
                etiqueta.setTextColor(colorInactivo)
            }
        }
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

        val prioridad = prioridadSeleccionada
        if (prioridad == null) {
            binding.txtPrioridadSeleccionada.text = getString(R.string.error_prioridad)
            binding.txtPrioridadSeleccionada.setTextColor(ContextCompat.getColor(this, R.color.prioridad_alta))
            return
        }

        val marcaTiempo = formatoHora.format(Date())

        incidencias.add(Incidencia(titulo, descripcion, prioridad, marcaTiempo))

        // Retroalimentacion visible inmediata a partir del estado capturado.
        binding.txtMensaje.text = getString(R.string.reporte_preparado, titulo)

        binding.edtTitulo.setText("")
        binding.edtDescripcion.setText("")
        actualizarPantalla()

        Snackbar.make(binding.root, R.string.msg_registrada, Snackbar.LENGTH_SHORT).show()
    }

    /** Borra todas las incidencias registradas y reinicia el contador, el mensaje y la prioridad elegida. */
    private fun limpiarRegistro() {
        incidencias.clear()
        binding.edtTitulo.setText("")
        binding.edtDescripcion.setText("")
        binding.inputLayoutTitulo.error = null
        binding.inputLayoutDescripcion.error = null
        binding.txtMensaje.text = getString(R.string.mensaje_inicial)

        prioridadSeleccionada = null
        binding.txtPrioridadSeleccionada.text = getString(R.string.prioridad_no_seleccionada)
        binding.txtPrioridadSeleccionada.setTextColor(ContextCompat.getColor(this, R.color.gris_texto))
        actualizarTarjetasPrioridad()

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
