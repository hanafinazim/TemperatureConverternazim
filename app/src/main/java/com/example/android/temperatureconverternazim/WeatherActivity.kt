package com.example.android.temperatureconverternazim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherActivity : AppCompatActivity() {

    private lateinit var editCityName: EditText
    private lateinit var buttonSearchCity: Button
    private lateinit var buttonOpenMap: Button
    private lateinit var textWeatherResult: TextView

    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ⚠️ ICI on utilise bien activity_weather.xml, pas activity_weather_map.xml
        setContentView(R.layout.activity_weather)

        editCityName = findViewById(R.id.editCityName)
        buttonSearchCity = findViewById(R.id.buttonSearchCity)
        buttonOpenMap = findViewById(R.id.buttonOpenMap)
        textWeatherResult = findViewById(R.id.textWeatherResult)

        // Bouton pour chercher la météo d'une ville
        buttonSearchCity.setOnClickListener {
            val city = editCityName.text.toString().trim()
            if (city.isEmpty()) {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            } else {
                searchCityWeather(city)
            }
        }

        // Bouton pour ouvrir la carte météo
        buttonOpenMap.setOnClickListener {
            val intent = Intent(this, WeatherMapActivity::class.java)
            startActivity(intent)
        }
    }

    // -------------------------------
    // 1) Géocodage de la ville → lat/lon
    // -------------------------------
    private fun searchCityWeather(city: String) {
        textWeatherResult.text = "Loading weather for $city..."

        val url =
            "https://geocoding-api.open-meteo.com/v1/search?name=$city&count=1&language=en&format=json"

        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    textWeatherResult.text = "Error: network problem"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            textWeatherResult.text = "Error: API problem (${response.code})"
                        }
                        return
                    }

                    val body = it.body?.string() ?: run {
                        runOnUiThread {
                            textWeatherResult.text = "Error: empty response"
                        }
                        return
                    }

                    val json = JSONObject(body)
                    if (!json.has("results")) {
                        runOnUiThread {
                            textWeatherResult.text = "City not found"
                        }
                        return
                    }

                    val result = json.getJSONArray("results").getJSONObject(0)
                    val lat = result.getDouble("latitude")
                    val lon = result.getDouble("longitude")
                    val cityName = result.getString("name")
                    val country = result.optString("country", "")

                    // On appelle ensuite la météo
                    fetchWeather(lat, lon, "$cityName, $country")
                }
            }
        })
    }

    // -------------------------------
    // 2) Météo pour des coordonnées
    // -------------------------------
    private fun fetchWeather(lat: Double, lon: Double, placeName: String) {
        val url =
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current_weather=true&timezone=auto"

        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    textWeatherResult.text = "Error: network problem"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            textWeatherResult.text = "Error: API problem (${response.code})"
                        }
                        return
                    }

                    val body = it.body?.string() ?: run {
                        runOnUiThread {
                            textWeatherResult.text = "Error: empty response"
                        }
                        return
                    }

                    val json = JSONObject(body)
                    val current = json.getJSONObject("current_weather")
                    val temp = current.getDouble("temperature")
                    val wind = current.getDouble("windspeed")
                    val code = current.getInt("weathercode")

                    val description = weatherDescription(code)

                    runOnUiThread {
                        textWeatherResult.text = """
                            $placeName
                            
                            Condition: $description
                            Temperature: %.1f °C
                            Wind: %.1f km/h
                        """.trimIndent().format(temp, wind)
                    }
                }
            }
        })
    }

    // -------------------------------
    // 3) Description des codes météo
    // -------------------------------
    private fun weatherDescription(code: Int): String =
        when (code) {
            0 -> "Clear sky ☀️"
            1, 2 -> "Partly cloudy 🌤"
            3 -> "Overcast ☁️"
            in 51..67 -> "Drizzle 🌦"
            in 71..77 -> "Snow ❄️"
            in 80..82 -> "Rain showers 🌧"
            in 95..99 -> "Thunderstorm ⛈"
            else -> "Unknown 🌍"
        }
}
