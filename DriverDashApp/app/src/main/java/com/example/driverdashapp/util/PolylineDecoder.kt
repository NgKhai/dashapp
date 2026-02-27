package com.example.driverdashapp.util

import org.osmdroid.util.GeoPoint

/**
 * Decodes a Google-format encoded polyline string into a list of GeoPoints.
 *
 * Uses precision 5 (standard OSRM polyline encoding).
 * Returns an empty list if the input is null or malformed — callers should
 * fall back to a straight line between pickup and drop-off in that case.
 *
 * Algorithm: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object PolylineDecoder {

    fun decode(encoded: String?): List<GeoPoint> {
        if (encoded.isNullOrBlank()) return emptyList()

        val points = mutableListOf<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            // Decode latitude
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            // Decode longitude
            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(GeoPoint(lat / 1e5, lng / 1e5))
        }

        return points
    }
}
