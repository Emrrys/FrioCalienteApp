package com.example.epe3

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.Manifest
import androidx.core.app.ActivityCompat
import android.annotation.SuppressLint
import android.location.Location

class MainActivity : AppCompatActivity(){
    //nos permite manejar el GPS y redes para mejorar la precision de la ubicacion
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //elementos de la vista activity_main.xml
    private lateinit var tvUbicacionActual: TextView
    private lateinit var tvEstadoBusqueda: TextView
    private lateinit var btnGuardarTesoro: Button
    private lateinit var etNombreTesoro: EditText

    //base de datos local SQLite
    private lateinit var dbHelper: DBHelper

    //manejo de solicitudes y recepcion de actualizaciones de ubicacion en tiempo real
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //varialbes para almacenar latitud y longitud actual
    private var latitudActual: Double = 0.0
    private var longitudActual: Double = 0.0

    //codigo de solicitud de permiso de ubicacion explicita
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate (savedInstanceState: Bundle?) {
        //inicializa componentes
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvEstadoBusqueda = findViewById(R.id.tvEstadoBusqueda)
        tvUbicacionActual = findViewById(R.id.tvUbicacionActual)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crearLocationRequest()
        crearLocationCallback()
        solicitarPermisosUbicacion()

        //asignacion de vistas
        dbHelper = DBHelper(this)
        etNombreTesoro = findViewById(R.id.etNombreTesoro)
        btnGuardarTesoro = findViewById(R.id.btnGuardarTesoro)

        //funcionalidad del boton
        btnGuardarTesoro.setOnClickListener {
            guardarTesoro()
        }
    }

    //solicitamos los permisos de ubicacion al usuario si no estan concedidos
    private fun solicitarPermisosUbicacion(){
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            obtenerUbicacionActual()
        }
    }

    //manejamos el resultado de la solicitud de permiso GPS
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionActual()
        } else {
            tvUbicacionActual.text = "Permiso de ubicación denegado."
        }
    }

    //Comienza a recibir actualizaciones de ubicacion si hay permisos de GPS
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    //configuramos el tipo de ubicacion y alta precision cada 10 segundos
    @SuppressLint("MissingPermission")
    private fun crearLocationRequest() {
        locationRequest = LocationRequest.Builder(10000) // intervalo 10 segundos
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    //definimos como se maneja cada ubicacion recibida, actualiza varaibles globales
    //y recalcula el estado de la busqueda
    private fun crearLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    latitudActual = it.latitude
                    longitudActual = it.longitude
                    tvUbicacionActual.text = "Ubicación actual:\nLat: $latitudActual\nLon: $longitudActual"
                    verificarEstado()
                }
            }
        }
    }

    // guardamos la ubicacion actual con un normbre de tesoro y generamos un primer estado
    // para comenzar con el juego
    private fun guardarTesoro() {
        val nombre = etNombreTesoro.text.toString()

        if (nombre.isEmpty()) {
            etNombreTesoro.error = "Ingresa el nombre del tesoro"
            return
        }

        val nuevoTesoro = Tesoro(
            nombre = nombre,
            latitud = latitudActual,
            longitud = longitudActual
        )

        dbHelper.insertarTesoro(nuevoTesoro)
        etNombreTesoro.text.clear()
        tvUbicacionActual.text = "Tesoro guardado: $nombre\nLat: $latitudActual\nLon: $longitudActual"
        mostrarTesorosGuardados()
        verificarEstado()
    }

    //esta es una validacion para saber los tesoros guardados mediante la consola
    private fun mostrarTesorosGuardados() {
        val tesoros = dbHelper.obtenerTesoros()

        for (tesoro in tesoros) {
            println("Tesoro: ${tesoro.nombre}, Latitud: ${tesoro.latitud}, Longitud: ${tesoro.longitud}")
        }
    }

    // funcion que nos ayuda a calculcar la distancia entre dos coordenadas cartecianas
    // le entregamos 4 parametros y retorna un float
    private fun calcularDistancia(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val resultado = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, resultado)
        return resultado[0] // devuelve distancia en metros
    }

    // Verificamos la distancia entre la ubicacion actual y el ultimo tesoro guardado
    // Actualizamos el estado de la busqueda en tiempo real (frio - tibio - caliente)
    private fun verificarEstado() {
        val tesoros = dbHelper.obtenerTesoros()
        if (tesoros.isNotEmpty()) {
            val ultimoTesoro = tesoros.last()

            val distancia = calcularDistancia(
                latitudActual, longitudActual,
                ultimoTesoro.latitud, ultimoTesoro.longitud
            )

            when {
                distancia > 100 -> {
                    tvEstadoBusqueda.text = "Estado: Frío ❄️"
                }
                distancia in 30.0..100.0 -> {
                    tvEstadoBusqueda.text = "Estado: Tibio 🌡️"
                }
                distancia <= 30 -> {
                    tvEstadoBusqueda.text = "Estado: Caliente 🔥"
                }
            }
        }
    }
}