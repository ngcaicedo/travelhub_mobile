package com.uniandes.travelhub.models.search

/**
 * Geographic viewport sent to /api/v1/search when the user searches "in this area"
 * from the mobile map view. The four values define the bbox corners.
 *
 * Invariants enforced at construction:
 *  - Latitudes are in [-90, 90] and minLat < maxLat.
 *  - Longitudes are in [-180, 180] and minLng < maxLng.
 */
data class SearchBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) {
    init {
        require(minLat in -90.0..90.0 && maxLat in -90.0..90.0) {
            "Latitude must be within [-90, 90]"
        }
        require(minLng in -180.0..180.0 && maxLng in -180.0..180.0) {
            "Longitude must be within [-180, 180]"
        }
        require(minLat < maxLat) { "minLat must be less than maxLat" }
        require(minLng < maxLng) { "minLng must be less than maxLng" }
    }

    /** Width/height of the box in degrees — useful to detect viewport drift. */
    val latSpan: Double get() = maxLat - minLat
    val lngSpan: Double get() = maxLng - minLng
}
