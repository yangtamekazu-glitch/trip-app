package com.example.dailytrips

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dailytrips.ui.theme.DailyTripsTheme
import com.example.dailytrips.ui.viewmodel.PlaceCategory
import com.example.dailytrips.ui.viewmodel.PlacesUiState
import com.example.dailytrips.ui.viewmodel.PlacesViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            DailyTripsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(fusedLocationClient)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: PlacesViewModel = viewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchRadius by viewModel.searchRadius.collectAsState()
    
    val context = LocalContext.current
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    var showSearchButton by remember { mutableStateOf(false) }

    // UIの状態管理
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState is PlacesUiState.Initial) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        userLocation = currentLatLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 14f)
                        viewModel.fetchPlacesNearby(location.latitude, location.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            showSearchButton = true
        }
    }

    // ==========================================
    // 設定ダイアログ（距離変更 ＆ カテゴリー編集）
    // ==========================================
    if (showSettingsDialog) {
        var newCategoryName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("表示設定", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("検索範囲: ${searchRadius.toInt() / 1000} km", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = searchRadius.toFloat(),
                        onValueChange = { viewModel.setSearchRadius(it.toDouble()) },
                        valueRange = 1000f..10000f,
                        steps = 8
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("カテゴリー編集 (${categories.size}/10個)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        itemsIndexed(categories) { index, category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text(category.displayName, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.reorderCategories(index, index - 1) },
                                    enabled = index > 0
                                ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上へ") }
                                IconButton(
                                    onClick = { viewModel.reorderCategories(index, index + 1) },
                                    enabled = index < categories.size - 1
                                ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下へ") }
                                
                                if (category.id != "recommend") {
                                    IconButton(onClick = { viewModel.removeCategory(category) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.Red)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(48.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("新規追加", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("カテゴリー名 (例: 温泉、絶景)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    
                    if (categories.size >= 10) {
                        Text("※カテゴリーは最大10個までです", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            viewModel.addCustomCategory(newCategoryName)
                            newCategoryName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newCategoryName.isNotBlank() && categories.size < 10
                    ) {
                        Text("このカテゴリーを追加")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("完了")
                }
            }
        )
    }

    // ==========================================
    // トップバー
    // ==========================================
    val topBarContent: @Composable () -> Unit = {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            TopAppBar(
                title = { 
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("キーワード検索") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("DailyTrips", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isSearchExpanded && searchQuery.isNotBlank()) {
                            viewModel.searchByKeyword(searchQuery)
                        }
                        isSearchExpanded = !isSearchExpanded 
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "検索")
                    }
                    if (!isSearchExpanded) {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "設定")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }
        }
    }

    // ==========================================
    // メイン画面（マップ ＆ ボトムシート）
    // ==========================================
    if (!hasPermission) {
        Scaffold(topBar = topBarContent) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues), 
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) {
                    Text(text = "位置情報の権限を許可してください")
                }
            }
        }
    } else {
        val scaffoldState = rememberBottomSheetScaffoldState()

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            topBar = topBarContent,
            sheetPeekHeight = 350.dp, 
            sheetContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState) {
                        is PlacesUiState.Initial, is PlacesUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is PlacesUiState.Error -> {
                            val error = (uiState as PlacesUiState.Error).message
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "エラーが発生しました: $error")
                            }
                        }
                        is PlacesUiState.Success -> {
                            val places = (uiState as PlacesUiState.Success).places
                            if (places.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "写真のあるスポットが見つかりませんでした。")
                                }
                            } else {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalItemSpacing = 12.dp
                                ) {
                                    items(places, key = { it.id }) { place ->
                                        val height = remember(place.id) { Random.nextInt(150, 300).dp }
                                        place.photos?.firstOrNull()?.let { photo ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(height)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable {
                                                        place.location?.let { loc ->
                                                            val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${Uri.encode(place.displayName?.text)})")
                                                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                                            mapIntent.setPackage("com.google.android.apps.maps")
                                                            context.startActivity(mapIntent)
                                                        }
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(viewModel.getPhotoUrl(photo.name))
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = place.displayName?.text,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                                startY = 100f
                                                            )
                                                        )
                                                )
                                                
                                                if (place.rating != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "⭐ ${place.rating}",
                                                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = place.displayName?.text ?: "Unknown Place",
                                                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                                                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()) 
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    // ★ここに追加しました！この1行で現在地表示がオンになります！
                    properties = MapProperties(isMyLocationEnabled = hasPermission),
                    uiSettings = MapUiSettings(
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = true,
                        compassEnabled = false
                    )
                ) {
                    if (uiState is PlacesUiState.Success) {
                        val places = (uiState as PlacesUiState.Success).places
                        places.forEach { place ->
                            place.location?.let { loc ->
                                Marker(
                                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                                    title = place.displayName?.text
                                )
                            }
                        }
                    }
                }

                if (showSearchButton && !cameraPositionState.isMoving) {
                    Button(
                        onClick = {
                            val target = cameraPositionState.position.target
                            viewModel.fetchPlacesNearby(target.latitude, target.longitude)
                            showSearchButton = false
                        },
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("このエリアを検索")
                    }
                }
            }
        }
    }
}