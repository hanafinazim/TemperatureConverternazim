package com.example.android.temperatureconverternazim

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver

fun MapView.setOnTapListener(onTap: (GeoPoint) -> Boolean) {
    val overlay = MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
            p?.let { onTap(it) }
            return true
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
            return false
        }
    })
    overlays.add(overlay)
}
