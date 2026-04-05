package com.example.dailytrips.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrips.data.Place
import com.example.dailytrips.data.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaceCategory(val id: String, val displayName: String, val types: List<String>)

class PlacesViewModel : ViewModel() {
    private val repository = PlacesRepository()

    private val _uiState = MutableStateFlow<PlacesUiState>(PlacesUiState.Initial)
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    private val defaultCategories = listOf(
        PlaceCategory("recommend", "おすすめ", listOf("tourist_attraction", "park", "cafe", "museum", "point_of_interest")),
        PlaceCategory("cafe", "カフェ", listOf("cafe")),
        PlaceCategory("nature", "自然", listOf("park", "national_park")),
        PlaceCategory("art", "アート", listOf("museum", "art_gallery")),
        PlaceCategory("food", "食事", listOf("restaurant"))
    )

    private val _categories = MutableStateFlow(defaultCategories)
    val categories: StateFlow<List<PlaceCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow(defaultCategories.first())
    val selectedCategory: StateFlow<PlaceCategory> = _selectedCategory.asStateFlow()

    // 検索距離（デフォルト3000m = 3km）
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
                // ★ 修正：設定された距離（_searchRadius.value）をRepositoryに渡す
                val places = repository.fetchNearbyPlaces(latitude, longitude, category.types, _searchRadius.value)
                var placesWithPhotos = places.filter { it.photos?.isNotEmpty() == true }
                
                // ▼▼ ここを「賢い絞り込み」に変更しました！ ▼▼
                if (category.id == "recommend") {
                    // 一旦、星4.0以上のものを探す
                    val highlyRated = placesWithPhotos.filter { (it.rating ?: 0.0) >= 4.0 }
                    
                    // もし星4以上が1つでも見つかればそれに絞る。0件なら妥協して元のリスト(星4未満も含む)を表示する。
                    if (highlyRated.isNotEmpty()) {
                        placesWithPhotos = highlyRated
                    }
                }
                // ▲▲ ここまで ▲▲
                
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
                // キーワード検索の時も設定距離を使う
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

    // ★ 新規追加：カスタムカテゴリーを追加する機能
    fun addCustomCategory(name: String, typesString: String) {
        val typesList = typesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (name.isNotBlank() && typesList.isNotEmpty()) {
            val newCategory = PlaceCategory(
                id = "custom_${System.currentTimeMillis()}",
                displayName = name,
                types = typesList
            )
            _categories.value = _categories.value + newCategory
        }
    }

    // ★ 新規追加：カテゴリーを削除する機能（"おすすめ"は消せないように保護）
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