package com.example.dailytrips.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrips.data.Place
import com.example.dailytrips.data.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 英語キーワード(types)が無くても、日本語キーワード(searchQuery)で検索できるように変更
data class PlaceCategory(
    val id: String, 
    val displayName: String, 
    val types: List<String> = emptyList(), 
    val searchQuery: String? = null
)

class PlacesViewModel : ViewModel() {
    private val repository = PlacesRepository()

    private val _uiState = MutableStateFlow<PlacesUiState>(PlacesUiState.Initial)
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    private val defaultCategories = listOf(
        // ★変更：すべてのデフォルトカテゴリーで「人気の〇〇」というキーワード検索（Text Search）が走るようにしました
        PlaceCategory("recommend", "おすすめ", searchQuery = "人気スポット"),
        PlaceCategory("cafe", "カフェ", searchQuery = "人気のカフェ"),
        PlaceCategory("nature", "自然", searchQuery = "人気の公園 自然"),
        PlaceCategory("art", "アート", searchQuery = "人気の美術館 ギャラリー"),
        PlaceCategory("food", "食事", searchQuery = "人気のレストラン 食事")
    )

    private val _categories = MutableStateFlow(defaultCategories)
    val categories: StateFlow<List<PlaceCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow(defaultCategories.first())
    val selectedCategory: StateFlow<PlaceCategory> = _selectedCategory.asStateFlow()

    private val _searchRadius = MutableStateFlow(3000.0)
    val searchRadius: StateFlow<Double> = _searchRadius.asStateFlow()

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    fun fetchPlacesNearby(latitude: Double, longitude: Double, category: PlaceCategory = _selectedCategory.value) {
        currentLat = latitude
        currentLng = longitude
        
        _uiState.value = PlacesUiState.Loading 
        viewModelScope.launch {
            try {
                // 日本語キーワードが設定されている場合は Text Search、それ以外は Nearby Search を使う
                val places = if (category.searchQuery != null) {
                    repository.searchPlacesByText(category.searchQuery, latitude, longitude, _searchRadius.value)
                } else {
                    repository.fetchNearbyPlaces(latitude, longitude, category.types, _searchRadius.value)
                }

                var placesWithPhotos = places.filter { it.photos?.isNotEmpty() == true }
                
                // 「おすすめ」の場合は星4.0以上に絞り込む
                if (category.id == "recommend") {
                    val highlyRated = placesWithPhotos.filter { (it.rating ?: 0.0) >= 4.0 }
                    if (highlyRated.isNotEmpty()) {
                        placesWithPhotos = highlyRated
                    }
                }
                
                _uiState.value = PlacesUiState.Success(placesWithPhotos)
            } catch (e: Exception) {
                _uiState.value = PlacesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun searchByKeyword(keyword: String) {
        if (keyword.isBlank() || currentLat == 0.0 || currentLng == 0.0) return
        _uiState.value = PlacesUiState.Loading
        viewModelScope.launch {
            try {
                val places = repository.searchPlacesByText(keyword, currentLat, currentLng, _searchRadius.value)
                val placesWithPhotos = places.filter { it.photos?.isNotEmpty() == true }
                _uiState.value = PlacesUiState.Success(placesWithPhotos)
            } catch (e: Exception) {
                _uiState.value = PlacesUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun selectCategory(category: PlaceCategory) {
        _selectedCategory.value = category
        if (currentLat != 0.0 && currentLng != 0.0) {
            fetchPlacesNearby(currentLat, currentLng, category)
        }
    }

    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        val currentList = _categories.value.toMutableList()
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _categories.value = currentList
    }

    // 日本語の名前だけで追加できるようにし、最大10個の制限を追加
    fun addCustomCategory(name: String) {
        if (_categories.value.size >= 10) return // 10個以上の場合は追加しない
        
        if (name.isNotBlank()) {
            val newCategory = PlaceCategory(
                id = "custom_${System.currentTimeMillis()}",
                displayName = name,
                // ★ユーザーが追加したカテゴリーも「人気の〇〇」として検索されるように調整
                searchQuery = "人気の $name" 
            )
            _categories.value = _categories.value + newCategory
        }
    }

    fun removeCategory(category: PlaceCategory) {
        if (category.id != "recommend") {
            _categories.value = _categories.value - category
            if (_selectedCategory.value == category) {
                _selectedCategory.value = _categories.value.first()
            }
        }
    }

    fun setSearchRadius(radius: Double) {
        _searchRadius.value = radius
        if (currentLat != 0.0 && currentLng != 0.0) {
            fetchPlacesNearby(currentLat, currentLng)
        }
    }

    fun getPhotoUrl(photoName: String): String = repository.getPhotoUrl(photoName)
}

sealed class PlacesUiState {
    object Initial : PlacesUiState()
    object Loading : PlacesUiState()
    data class Success(val places: List<Place>) : PlacesUiState()
    data class Error(val message: String) : PlacesUiState()
}