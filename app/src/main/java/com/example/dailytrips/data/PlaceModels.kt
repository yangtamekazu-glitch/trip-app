package com.example.dailytrips.data

import com.google.gson.annotations.SerializedName

data class SearchNearbyRequest(
    val includedTypes: List<String>,
    val maxResultCount: Int,
    val locationRestriction: LocationRestriction
)

data class LocationRestriction(
    val circle: Circle
)

data class Circle(
    val center: LatLng,
    val radius: Double
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class SearchNearbyResponse(
    val places: List<Place>?
)

// ★ ここが修正のメインです！一番最後に rating を追加しました！
data class Place(
    val id: String,
    val displayName: DisplayName?,
    val photos: List<Photo>?,
    val location: LatLng?,
    val rating: Double? = null 
)

data class DisplayName(
    val text: String,
    val languageCode: String?
)

data class Photo(
    val name: String, // format: "places/{placeId}/photos/{photo_reference}"
    val widthPx: Int,
    val heightPx: Int,
    val authorAttributions: List<AuthorAttribution>?
)

data class AuthorAttribution(
    val displayName: String,
    val uri: String,
    val photoUri: String
)