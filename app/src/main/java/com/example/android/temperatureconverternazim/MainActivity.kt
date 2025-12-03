package com.example.android.temperatureconverternazim

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

enum class TemperatureUnit(val displayName: String) {
    CELSIUS("Celsius (°C)"),
    FAHRENHEIT("Fahrenheit (°F)"),
    KELVIN("Kelvin (K)");

    companion object {
        fun fromDisplayName(name: String): TemperatureUnit? {
            return values().find { it.displayName == name }
        }
    }
}

data class FavoriteConversion(
    val from: TemperatureUnit,
    val to: TemperatureUnit
) {
    override fun toString(): String {
        return "${from.displayName} → ${to.displayName}"
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var editTextValue: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var buttonConvert: Button
    private lateinit var textViewResult: TextView
    private lateinit var buttonAddFavorite: Button
    private lateinit var listViewFavorites: ListView

    private val favorites = mutableListOf<FavoriteConversion>()
    private lateinit var favoritesAdapter: ArrayAdapter<FavoriteConversion>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Liaison des vues
        editTextValue = findViewById(R.id.editTextValue)
        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)
        buttonConvert = findViewById(R.id.buttonConvert)
        textViewResult = findViewById(R.id.textViewResult)
        buttonAddFavorite = findViewById(R.id.buttonAddFavorite)
        listViewFavorites = findViewById(R.id.listViewFavorites)

        // Remplir les spinners avec les unités
        val unitNames = TemperatureUnit.values().map { it.displayName }
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            unitNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = spinnerAdapter
        spinnerTo.adapter = spinnerAdapter

        // Valeurs par défaut
        spinnerFrom.setSelection(TemperatureUnit.CELSIUS.ordinal)
        spinnerTo.setSelection(TemperatureUnit.FAHRENHEIT.ordinal)

        // Bouton Convert
        buttonConvert.setOnClickListener {
            val inputText = editTextValue.text.toString()
            if (inputText.isEmpty()) {
                Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val value = inputText.toDoubleOrNull()
            if (value == null) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fromUnitName = spinnerFrom.selectedItem as String
            val toUnitName = spinnerTo.selectedItem as String

            val fromUnit = TemperatureUnit.fromDisplayName(fromUnitName)!!
            val toUnit = TemperatureUnit.fromDisplayName(toUnitName)!!

            val result = convertTemperature(value, fromUnit, toUnit)

            textViewResult.text = "Result: %.2f".format(result)
        }

        // Adapter pour les favoris
        favoritesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            favorites
        )
        listViewFavorites.adapter = favoritesAdapter

        // Bouton "Add to favorites"
        buttonAddFavorite.setOnClickListener {
            val fromUnitName = spinnerFrom.selectedItem as String
            val toUnitName = spinnerTo.selectedItem as String

            val fromUnit = TemperatureUnit.fromDisplayName(fromUnitName)!!
            val toUnit = TemperatureUnit.fromDisplayName(toUnitName)!!

            val newFavorite = FavoriteConversion(fromUnit, toUnit)

            if (favorites.contains(newFavorite)) {
                Toast.makeText(this, "Favorite already exists", Toast.LENGTH_SHORT).show()
            } else {
                favorites.add(newFavorite)
                favoritesAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        }

        // Cliquer sur un favori => recharge la conversion
        listViewFavorites.setOnItemClickListener { _, _, position, _ ->
            val favorite = favorites[position]
            spinnerFrom.setSelection(favorite.from.ordinal)
            spinnerTo.setSelection(favorite.to.ordinal)
            Toast.makeText(
                this,
                "Favorite selected: ${favorite.from.displayName} → ${favorite.to.displayName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Conversion générique
    private fun convertTemperature(
        value: Double,
        from: TemperatureUnit,
        to: TemperatureUnit
    ): Double {
        if (from == to) return value

        // 1) vers Celsius
        val valueInCelsius = when (from) {
            TemperatureUnit.CELSIUS -> value
            TemperatureUnit.FAHRENHEIT -> (value - 32.0) * 5.0 / 9.0
            TemperatureUnit.KELVIN -> value - 273.15
        }

        // 2) de Celsius vers cible
        return when (to) {
            TemperatureUnit.CELSIUS -> valueInCelsius
            TemperatureUnit.FAHRENHEIT -> valueInCelsius * 9.0 / 5.0 + 32.0
            TemperatureUnit.KELVIN -> valueInCelsius + 273.15
        }
    }
}
