package com.example.dailytrips.data

import com.example.dailytrips.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlacesRepository {

    private val apiService: PlacesApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(PlacesApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(PlacesApiService::class.java)
    }

    // カテゴリ検索
    // ★変更：antiGravityを削除し、radius: Double を追加
    suspend fun fetchNearbyPlaces(latitude: Double, longitude: Double, types: List<String>, radius: Double): List<Place> {
        val request = SearchNearbyRequest(
            includedTypes = types,
            maxResultCount = 20,
            locationRestriction = LocationRestriction(
                // ★変更：固定値ではなく、スライダーで設定した radius を使う
                circle = Circle(center = LatLng(latitude, longitude), radius = radius)
            )
        )
        return try {
            val response = apiService.searchNearby(
                apiKey = BuildConfig.MAPS_API_KEY,
                // ★変更：最後に ,places.rating を追加して星の評価を受け取る
                fieldMask = "places.id,places.displayName,places.photos,places.location,places.rating",
                request = request
            )
            response.places ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // キーワード検索 (searchText)
    // ★変更：引数に radius: Double を追加
    suspend fun searchPlacesByText(query: String, latitude: Double, longitude: Double, radius: Double): List<Place> {
        val request = SearchTextRequest(
            textQuery = query,
            locationBias = LocationBias(
                // ★変更：固定値ではなく、スライダーで設定した radius を使う
                circle = Circle(center = LatLng(latitude, longitude), radius = radius)
            )
        )
        return try {
            val response = apiService.searchText(
                apiKey = BuildConfig.MAPS_API_KEY,
                // ★変更：最後に ,places.rating を追加して星の評価を受け取る
                fieldMask = "places.id,places.displayName,places.photos,places.location,places.rating",
                request = request
            )
            response.places ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getPhotoUrl(photoName: String, maxWidth: Int = 800): String {
        return "${PlacesApiService.BASE_URL}v1/$photoName/media?maxWidthPx=$maxWidth&key=${BuildConfig.MAPS_API_KEY}"
    }
}