package com.example.android.temperatureconverternazim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // --- UI convertisseur ---
    private lateinit var editTextValue: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var buttonConvert: Button
    private lateinit var textViewResult: TextView

    // --- UI favoris ---
    private lateinit var listViewFavorites: ListView
    private lateinit var buttonAddFavorite: Button
    private val favorites = mutableListOf<String>()
    private lateinit var favoritesAdapter: ArrayAdapter<String>

    // --- UI météo locale ---
    private lateinit var buttonGetWeather: Button
    private lateinit var textViewWeather: TextView

    // --- Météo / localisation ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val httpClient = OkHttpClient()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // -----------------------------
        // BIND DES VUES
        // -----------------------------
        editTextValue = findViewById(R.id.editTextValue)
        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)
        buttonConvert = findViewById(R.id.buttonConvert)
        textViewResult = findViewById(R.id.textViewResult)

        buttonAddFavorite = findViewById(R.id.buttonAddFavorite)
        listViewFavorites = findViewById(R.id.listViewFavorites)

        buttonGetWeather = findViewById(R.id.buttonGetWeather)
        textViewWeather = findViewById(R.id.textViewWeather)

        // -----------------------------
        // CLIENT LOCALISATION
        // -----------------------------
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // -----------------------------
        // SPINNERS D’UNITÉS
        // -----------------------------
        val units = arrayOf("Celsius", "Fahrenheit", "Kelvin")
        val unitsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units
        )
        unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = unitsAdapter
        spinnerTo.adapter = unitsAdapter

        // -----------------------------
        // LISTE DE FAVORIS
        // -----------------------------
        favoritesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            favorites
        )
        listViewFavorites.adapter = favoritesAdapter

        // -----------------------------
        // BOUTON CONVERTIR
        // -----------------------------
        buttonConvert.setOnClickListener {
            convertTemperature()
        }

        // -----------------------------
        // BOUTON AJOUT FAVORI
        // -----------------------------
        buttonAddFavorite.setOnClickListener {
            val resultText = textViewResult.text.toString()
            if (resultText.contains(":")) {
                favorites.add(resultText)
                favoritesAdapter.notifyDataSetChanged()
            }
        }

        // -----------------------------
        // BOUTON MÉTÉO LOCALE
        // -----------------------------
        buttonGetWeather.setOnClickListener {
            checkLocationAndFetchWeather()
        }
    }

    // -----------------------------------------------------
    // 🔥 FONCTION DE CONVERSION
    // -----------------------------------------------------
    private fun convertTemperature() {
        val inputText = editTextValue.text.toString()
        if (inputText.isEmpty()) {
            textViewResult.text = "Please enter a value"
            return
        }

        val value = inputText.toDouble()
        val from = spinnerFrom.selectedItem.toString()
        val to = spinnerTo.selectedItem.toString()

        val result = when (from to to) {
            "Celsius" to "Fahrenheit" -> value * 9 / 5 + 32
            "Celsius" to "Kelvin" -> value + 273.15
            "Fahrenheit" to "Celsius" -> (value - 32) * 5 / 9
            "Fahrenheit" to "Kelvin" -> (value - 32) * 5 / 9 + 273.15
            "Kelvin" to "Celsius" -> value - 273.15
            "Kelvin" to "Fahrenheit" -> (value - 273.15) * 9 / 5 + 32
            else -> value
        }

        textViewResult.text = "Result: %.2f $to".format(result)
    }

    // -----------------------------------------------------
    // 🌤️ PARTIE MÉTÉO : PERMISSIONS + RÉCUP LOCATION
    // -----------------------------------------------------
    private fun checkLocationAndFetchWeather() {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
        ) {
            fetchWeatherWithLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchWeatherWithLocation()
        } else {
            textViewWeather.text = "Weather: permission denied"
        }
    }

    // -----------------------------------------------------
    // 🌍 RÉCUPÉRATION MÉTÉO AVEC OPEN-METEO
    // -----------------------------------------------------
    private fun fetchWeatherWithLocation() {
        textViewWeather.text = "Weather: loading..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                textViewWeather.text = "Weather: location unavailable"
                return@addOnSuccessListener
            }

            val latitude = location.latitude
            val longitude = location.longitude

            val url =
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude" +
                        "&longitude=$longitude" +
                        "&current_weather=true"

            val request = Request.Builder()
                .url(url)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        textViewWeather.text = "Weather: connection error"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            runOnUiThread {
                                textViewWeather.text = "Weather: API error"
                            }
                            return
                        }

                        val body = response.body?.string() ?: run {
                            runOnUiThread {
                                textViewWeather.text = "Weather: empty response"
                            }
                            return
                        }

                        val json = JSONObject(body)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        val wind = current.getDouble("windspeed")
                        val code = current.getInt("weathercode")
                        val description = weatherCodeToText(code)

                        runOnUiThread {
                            textViewWeather.text =
                                "$description\nTemperature: %.1f °C\nWind: %.1f km/h"
                                    .format(temp, wind)
                        }
                    }
                }
            })
        }.addOnFailureListener {
            textViewWeather.text = "Weather: location error"
        }
    }

    // -----------------------------------------------------
    // ☁️ TRADUCTION DES CODES MÉTÉO OPEN-METEO
    // -----------------------------------------------------
    private fun weatherCodeToText(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2 -> "Mainly clear"
            3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown weather"
        }
    }
}
