package com.example.android.temperatureconverternazim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val buttonStart: Button = findViewById(R.id.buttonStartConverter)
        val buttonOpenWeather: Button = findViewById(R.id.buttonOpenWeather)
        val buttonOpenMap: Button = findViewById(R.id.buttonOpenMap)

        buttonStart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonOpenWeather.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }

        buttonOpenMap.setOnClickListener {
            startActivity(Intent(this, WeatherMapActivity::class.java))
        }
    }
}
