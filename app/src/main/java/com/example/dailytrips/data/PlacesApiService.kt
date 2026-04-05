package com.example.dailytrips.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PlacesApiService {

    /**
     * 周辺検索 (カテゴリ検索用)
     */
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Header("Accept-Language") language: String = "ja",
        @Body request: SearchNearbyRequest
    ): SearchNearbyResponse

    /**
     * テキスト検索 (自由なキーワード検索用)
     */
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Header("Accept-Language") language: String = "ja",
        @Body request: SearchTextRequest
    ): SearchNearbyResponse

    companion object {
        const val BASE_URL = "https://places.googleapis.com/"
    }
}

// --- 以下、テキスト検索に必要なデータクラス ---

data class SearchTextRequest(
    val textQuery: String,
    val locationBias: LocationBias? = null,
    val maxResultCount: Int = 20
)

data class LocationBias(
    val circle: Circle
)

// ※ もし Circle や LatLng が SearchNearbyRequest ですでに定義されている場合は
// 重複して書かないように注意してください。