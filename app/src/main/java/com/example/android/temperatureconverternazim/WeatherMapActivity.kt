package com.example.android.temperatureconverternazim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException

class WeatherMapActivity : AppCompatActivity(), MapEventsReceiver {

    private lateinit var mapView: MapView
    private lateinit var textMapInfo: TextView

    private val httpClient = OkHttpClient()

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val markers = mutableListOf<Marker>()
    private var lastMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid configuration (pas de PreferenceManager)
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_weather_map)

        mapView = findViewById(R.id.mapView)
        textMapInfo = findViewById(R.id.textMapInfo)

        setupMap()
        enableUserLocation()

        val mapEventsOverlay = MapEventsOverlay(this)
        mapView.overlays.add(mapEventsOverlay)
    }

    // ---------------------------------------------------------
    // CONFIGURATION DE LA CARTE
    // ---------------------------------------------------------
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(48.8566, 2.3522) // Paris
        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(startPoint)
    }

    // ---------------------------------------------------------
    // ACTIVE LA POSITION GPS
    // ---------------------------------------------------------
    private fun enableUserLocation() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                100
            )
            return
        }

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        myLocationOverlay!!.enableMyLocation()
        mapView.overlays.add(myLocationOverlay)
    }

    // ---------------------------------------------------------
    // CLIC SUR LA CARTE : afficher météo
    // ---------------------------------------------------------
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        if (p != null) fetchWeatherAt(p.latitude, p.longitude)
        return true
    }

    // ---------------------------------------------------------
    // APPUI LONG SUR LA CARTE : supprimer le dernier marker
    // ---------------------------------------------------------
    override fun longPressHelper(p: GeoPoint?): Boolean {
        lastMarker?.let {
            mapView.overlays.remove(it)
            markers.remove(it)
            lastMarker = null
            mapView.invalidate()
            textMapInfo.text = "Marker removed."
        }
        return true
    }

    // ---------------------------------------------------------
    // APPEL API OPEN-METEO
    // ---------------------------------------------------------
    private fun fetchWeatherAt(lat: Double, lon: Double) {
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"

        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    textMapInfo.text = "Weather request failed."
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body!!.string())
                val current = json.getJSONObject("current_weather")

                val temp = current.getDouble("temperature")
                val wind = current.getDouble("windspeed")

                runOnUiThread {
                    showWeatherMarker(lat, lon, "Weather", temp, wind)
                }
            }
        })
    }

    // ---------------------------------------------------------
    // CREATION DU MARKER METEO
    // ---------------------------------------------------------
    private fun showWeatherMarker(lat: Double, lon: Double, label: String, temp: Double, wind: Double) {
        val point = GeoPoint(lat, lon)

        val marker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Bulle d'information
            title = "$label\nTemp: $temp°C\nWind: $wind km/h"

            // Clic → afficher la bulle
            setOnMarkerClickListener { m, _ ->
                m.showInfoWindow()
                true
            }
        }

        // Nettoyer l'ancien marker si tu veux qu'il y en ait toujours 1 seul
        lastMarker?.let { mapView.overlays.remove(it) }

        mapView.overlays.add(marker)
        mapView.invalidate()

        lastMarker = marker
        markers.add(marker)

        textMapInfo.text = "⛅ Weather: $temp°C — Wind $wind km/h\n(Tap for more, Long press to remove)"
    }
}
