package com.example.registroincidencias

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.example.registroincidencias.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

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
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    /** Lista en memoria con las incidencias registradas en la sesion actual. */
    private val incidencias = mutableListOf<Incidencia>()

    /** Formato de hora usado para marcar cada incidencia. */
    private val formatoHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Gestor de sensores del sistema y acelerometro por defecto (null si el dispositivo no lo tiene). */
    private var sensorManager: SensorManager? = null
    private var acelerometro: Sensor? = null
    private var sensorLuz: Sensor? = null
    private var sensorProximidad: Sensor? = null

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
        Log.d(TAG_CICLO_VIDA, "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarSensores()

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

    // ------------------------------------------------------------------
    // Diagnostico del dispositivo (SensorManager + SensorEventListener)
    // ------------------------------------------------------------------

    /**
     * Comprueba la disponibilidad de cada recurso y deja la seccion de
     * diagnostico en su estado inicial. Si un sensor no existe en el
     * dispositivo, se informa la limitacion en lugar de dejar el espacio vacio.
     */
    private fun inicializarSensores() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorLuz = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorProximidad = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (acelerometro == null) {
            binding.txtSensorEstado.text = getString(R.string.sensor_no_disponible)
            binding.txtSensorValores.text = ""
            binding.txtSensorMensaje.text = ""
        } else {
            binding.txtSensorEstado.text = getString(R.string.sensor_disponible)
            binding.txtSensorValores.text = getString(R.string.sensor_esperando)
            binding.txtSensorMensaje.text = ""
        }

        if (sensorLuz == null) {
            binding.txtLuzEstado.text = getString(R.string.luz_no_disponible)
            binding.txtLuzValores.text = ""
            binding.txtLuzMensaje.text = ""
        } else {
            binding.txtLuzEstado.text = getString(R.string.luz_disponible)
            binding.txtLuzValores.text = getString(R.string.sensor_esperando)
            binding.txtLuzMensaje.text = ""
        }

        if (sensorProximidad == null) {
            binding.txtProximidadEstado.text = getString(R.string.proximidad_no_disponible)
            binding.txtProximidadValores.text = ""
            binding.txtProximidadMensaje.text = ""
        } else {
            binding.txtProximidadEstado.text = getString(R.string.proximidad_disponible)
            binding.txtProximidadValores.text = getString(R.string.sensor_esperando)
            binding.txtProximidadMensaje.text = ""
        }
    }

    /** Registra los listeners solo mientras la pantalla esta visible. */
    override fun onResume() {
        super.onResume()
        Log.d(TAG_CICLO_VIDA, "onResume")
        listOfNotNull(acelerometro, sensorLuz, sensorProximidad).forEach { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /** Libera todos los recursos al salir de la pantalla para no gastar bateria. */
    override fun onPause() {
        Log.d(TAG_CICLO_VIDA, "onPause")
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    /** La Activity vuelve a ser visible; precede a onStart al regresar desde segundo plano. */
    override fun onRestart() {
        super.onRestart()
        Log.d(TAG_CICLO_VIDA, "onRestart")
    }

    /** La Activity pasa a ser visible para el usuario. */
    override fun onStart() {
        super.onStart()
        Log.d(TAG_CICLO_VIDA, "onStart")
    }

    /** La Activity deja de ser visible (por ejemplo, al presionar Home). */
    override fun onStop() {
        Log.d(TAG_CICLO_VIDA, "onStop")
        super.onStop()
    }

    /** La Activity va a ser destruida (Back, rotacion o decision del sistema). */
    override fun onDestroy() {
        Log.d(TAG_CICLO_VIDA, "onDestroy")
        super.onDestroy()
    }

    /** Recibe cada lectura y la traduce a valores + interpretacion legible. */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> mostrarMovimiento(event)
            Sensor.TYPE_LIGHT -> mostrarLuz(event)
            Sensor.TYPE_PROXIMITY -> mostrarProximidad(event)
        }
    }

    /** Acelerometro: valores X/Y/Z en m/s2 e interpretacion del movimiento. */
    private fun mostrarMovimiento(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        binding.txtSensorValores.text = getString(R.string.sensor_valores_formato, x, y, z)

        // En reposo la magnitud ronda la gravedad (~9.81 m/s2); la desviacion indica movimiento.
        val magnitud = sqrt(x * x + y * y + z * z)
        val desviacion = abs(magnitud - SensorManager.GRAVITY_EARTH)

        binding.txtSensorMensaje.text = when {
            desviacion > UMBRAL_MOVIMIENTO_FUERTE -> getString(R.string.sensor_msg_fuerte)
            desviacion > UMBRAL_MOVIMIENTO_MODERADO -> getString(R.string.sensor_msg_moderado)
            else -> getString(R.string.sensor_msg_estable)
        }
    }

    /** Sensor de luz: lectura en lux e interpretacion del ambiente. */
    private fun mostrarLuz(event: SensorEvent) {
        val lux = event.values[0]

        binding.txtLuzValores.text = getString(R.string.luz_valores_formato, lux)
        binding.txtLuzMensaje.text = when {
            lux < UMBRAL_LUZ_OSCURO -> getString(R.string.luz_msg_oscuro)
            lux < UMBRAL_LUZ_BRILLANTE -> getString(R.string.luz_msg_moderado)
            else -> getString(R.string.luz_msg_brillante)
        }
    }

    /**
     * Proximidad: la mayoria de dispositivos reporta solo dos estados
     * (cerca / lejos), por lo que se compara contra el rango maximo del sensor.
     */
    private fun mostrarProximidad(event: SensorEvent) {
        val distancia = event.values[0]
        val rangoMaximo = sensorProximidad?.maximumRange ?: distancia

        binding.txtProximidadValores.text = getString(R.string.proximidad_valores_formato, distancia)
        binding.txtProximidadMensaje.text = if (distancia < rangoMaximo) {
            getString(R.string.proximidad_msg_cerca)
        } else {
            getString(R.string.proximidad_msg_lejos)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere manejar cambios de precision en esta integracion.
    }

    private companion object {
        /** Etiqueta usada para filtrar el ciclo de vida en Logcat. */
        const val TAG_CICLO_VIDA = "TPIS1_LIFECYCLE"

        const val UMBRAL_MOVIMIENTO_MODERADO = 1.5f
        const val UMBRAL_MOVIMIENTO_FUERTE = 6.0f
        const val UMBRAL_LUZ_OSCURO = 10f
        const val UMBRAL_LUZ_BRILLANTE = 2000f
    }
}
