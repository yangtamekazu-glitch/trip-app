import { useState, useCallback, useEffect } from 'react';
import { GoogleMap, useJsApiLoader, Marker } from '@react-google-maps/api';
import { Search, Settings, MapPin, Navigation2 } from 'lucide-react';
import './index.css';

const libraries = ['places'];
const mapContainerStyle = {
  width: '100%',
  height: '100%'
};

const defaultCenter = {
  lat: 35.6812, // Tokyo Station as fallback
  lng: 139.7671
};

const DEFAULT_CATEGORIES = [
  { id: 'recommend', displayName: 'おすすめ', searchContext: 'tourist attraction' },
  { id: 'cafe', displayName: 'カフェ', searchContext: 'cafe' },
  { id: 'restaurant', displayName: 'レストラン', searchContext: 'restaurant' },
  { id: 'park', displayName: '公園', searchContext: 'park' },
  { id: 'museum', displayName: '美術館・博物館', searchContext: 'museum' },
];

function App() {
  const { isLoaded, loadError } = useJsApiLoader({
    id: 'google-map-script',
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries
  });

  const [map, setMap] = useState(null);
  const [userLocation, setUserLocation] = useState(null);
  const [center, setCenter] = useState(defaultCenter);
  const [places, setPlaces] = useState([]);
  
  // Custom categories state
  const [categories, setCategories] = useState(() => {
    const saved = localStorage.getItem('tripApp_categories');
    return saved ? JSON.parse(saved) : DEFAULT_CATEGORIES;
  });
  const [selectedCategory, setSelectedCategory] = useState(categories[0]);
  
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState(null);

  // Settings and Search Modals
  const [showSettings, setShowSettings] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  // Theme state
  const [isDarkMode, setIsDarkMode] = useState(() => {
    const saved = localStorage.getItem('tripApp_theme');
    return saved !== 'light'; // Default is dark
  });

  // Sheet drag state
  const [sheetHeightVh, setSheetHeightVh] = useState(40);
  const [isDragging, setIsDragging] = useState(false);
  const [startY, setStartY] = useState(0);

  // Apply theme to document root
  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.removeAttribute('data-theme');
      localStorage.setItem('tripApp_theme', 'dark');
    } else {
      document.documentElement.setAttribute('data-theme', 'light');
      localStorage.setItem('tripApp_theme', 'light');
    }
  }, [isDarkMode]);

  const handlePointerDown = (e) => {
    setStartY(e.clientY);
    setIsDragging(true);
    e.target.setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e) => {
    if (!isDragging) return;
    const currentY = e.clientY;
    const windowH = window.innerHeight;
    const newHeightVh = ((windowH - currentY) / windowH) * 100;
    
    // Clamp height between 20vh and 90vh
    if (newHeightVh >= 20 && newHeightVh <= 90) {
      setSheetHeightVh(newHeightVh);
    }
  };

  const handlePointerUp = (e) => {
    setIsDragging(false);
    e.target.releasePointerCapture(e.pointerId);

    // Simple click logic: toggle between 40 and 85 if almost no drag occurred
    const deltaY = e.clientY - startY;
    if (Math.abs(deltaY) < 5) {
      setSheetHeightVh(sheetHeightVh > 60 ? 40 : 85);
    }
  };

  // Get user location on mount
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const loc = { lat: pos.coords.latitude, lng: pos.coords.longitude };
          setUserLocation(loc);
          setCenter(loc);
        },
        (err) => {
          console.error("Needs location permission", err);
        }
      );
    }
  }, []);

  const onLoad = useCallback(function callback(map) {
    setMap(map);
  }, []);

  const onUnmount = useCallback(function callback(map) {
    setMap(null);
  }, []);

  // Fetch places
  const fetchPlaces = useCallback(() => {
    if (!map) return;
    setIsSearching(true);
    setError(null);

    const service = new window.google.maps.places.PlacesService(map);
    const request = {
      location: center,
      radius: '2000',
      keyword: selectedCategory.searchContext,
    };

    service.nearbySearch(request, (results, status) => {
      setIsSearching(false);
      if (status === window.google.maps.places.PlacesServiceStatus.OK && results) {
        // Filter places with photos
        let withPhotos = results.filter(p => p.photos && p.photos.length > 0);
        
        // おすすめ順（評価スコア順）にソート
        withPhotos.sort((a, b) => {
           const scoreA = (a.rating || 0) * Math.log10(a.user_ratings_total || 10);
           const scoreB = (b.rating || 0) * Math.log10(b.user_ratings_total || 10);
           return scoreB - scoreA;
        });

        setPlaces(withPhotos);
      } else {
        setPlaces([]);
      }
    });
  }, [map, center, selectedCategory]);

  // Fetch when map or category changes
  useEffect(() => {
    if (map) {
      fetchPlaces();
    }
  }, [map, selectedCategory, fetchPlaces]);

  // Handle map drag end
  const handleDragEnd = () => {
    if (map) {
      const newCenter = map.getCenter();
      setCenter({ lat: newCenter.lat(), lng: newCenter.lng() });
    }
  };

  // Settings & Search Handlers
  const handleAddTag = () => {
    if (!newTagName.trim()) return;
    const newTag = {
      id: `custom_${Date.now()}`,
      displayName: newTagName,
      searchContext: newTagName
    };
    const updated = [...categories, newTag];
    setCategories(updated);
    localStorage.setItem('tripApp_categories', JSON.stringify(updated));
    setSelectedCategory(newTag);
    setNewTagName('');
    setShowSettings(false);
  };

  const handleSearchSubmit = () => {
    if (!searchKeyword.trim()) return;
    const tempCategory = {
      id: 'temp_search',
      displayName: `検索: ${searchKeyword}`,
      searchContext: searchKeyword
    };
    setSelectedCategory(tempCategory);
    setSearchKeyword('');
    setShowSearch(false);
  };

  // Routing External
  const openRoute = (place) => {
    const lat = place.geometry.location.lat();
    const lng = place.geometry.location.lng();
    const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
    window.open(url, '_blank');
  };

  const openInRunningApp = (place) => {
    const lat = place.geometry.location.lat();
    const lng = place.geometry.location.lng();
    const name = encodeURIComponent(place.name || '');
    let baseUrl = import.meta.env.VITE_RUNNING_APP_URL || `https://${window.location.hostname}:5000`;
    baseUrl = baseUrl.replace(/\/$/, '');
    const url = `${baseUrl}/?waypoint_lat=${lat}&waypoint_lng=${lng}&waypoint_name=${name}`;
    window.open(url, '_blank');
  };

  if (loadError) return <div className="center-flex text-white">Map Cannot Load Right Now</div>;
  if (!isLoaded) return <div className="center-flex text-white"><div className="spinner"></div></div>;

  return (
    <>
      <div className="map-container">
        <GoogleMap
          mapContainerStyle={mapContainerStyle}
          center={center}
          zoom={14}
          onLoad={onLoad}
          onUnmount={onUnmount}
          onDragEnd={handleDragEnd}
          options={{
            disableDefaultUI: true,
            styles: isDarkMode ? [ // Dark mode style
              { elementType: 'geometry', stylers: [{ color: '#242f3e' }] },
              { elementType: 'labels.text.stroke', stylers: [{ color: '#242f3e' }] },
              { elementType: 'labels.text.fill', stylers: [{ color: '#746855' }] },
              { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#17263c' }] }
            ] : [] // Light mode (default)
          }}
        >
          {userLocation && (
            <Marker 
              position={userLocation} 
              icon={{
                path: window.google.maps.SymbolPath.CIRCLE,
                scale: 7,
                fillColor: "#4285F4",
                fillOpacity: 1,
                strokeColor: "white",
                strokeWeight: 2,
              }}
            />
          )}
          {places.map((place) => (
            <Marker key={place.place_id} position={{ lat: place.geometry.location.lat(), lng: place.geometry.location.lng() }} />
          ))}
        </GoogleMap>
      </div>

      <div className="top-bar-overlay">
        <div className="top-bar-content glass-panel" style={{ borderRadius: '16px', padding: '12px', marginBottom: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h1 style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>DailyTrips</h1>
          <div style={{ display: 'flex', gap: '12px' }}>
            <Search size={20} color={isDarkMode ? "white" : "black"} style={{ cursor: 'pointer' }} onClick={() => setShowSearch(true)} />
            <Settings size={20} color={isDarkMode ? "white" : "black"} style={{ cursor: 'pointer' }} onClick={() => setShowSettings(true)} />
          </div>
        </div>

        <div className="category-scroll top-bar-content">
          {categories.map(cat => (
            <div 
              key={cat.id} 
              className={`glass-pill ${selectedCategory.id === cat.id ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat.displayName}
            </div>
          ))}
        </div>
        
        <div className="top-bar-content" style={{ display: 'flex', justifyContent: 'center', marginTop: '16px' }}>
          <button className="glass-pill" style={{ background: 'var(--primary-color)', color: 'white' }} onClick={fetchPlaces}>
            このエリアを再検索
          </button>
        </div>
      </div>

      {/* Bottom Sheet */}
      <div 
        className={`bottom-sheet ${isDragging ? 'dragging' : ''}`}
        style={{ height: `${sheetHeightVh}vh` }}
      >
        <div 
          className="bottom-sheet-handle" 
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
        ></div>
        
        <div className="bottom-sheet-content">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ fontSize: '1.2rem', fontWeight: '600' }}>{selectedCategory.displayName} ({places.length})</h2>
            <Navigation2 size={20} color="var(--primary-color)" />
          </div>

          {isSearching ? (
            <div className="center-flex" style={{ height: '200px' }}><div className="spinner"></div></div>
          ) : places.length === 0 ? (
            <div className="center-flex text-secondary" style={{ height: '200px' }}>写真のあるスポットが見つかりませんでした。</div>
          ) : (
            <div className="places-grid">
              {places.map((place, idx) => (
                <div 
                  key={place.place_id} 
                  className="place-card" 
                  style={{ height: `${150 + (idx % 3) * 30}px`, cursor: 'default' }}
                >
                  <img src={place.photos[0].getUrl({ maxWidth: 400 })} alt={place.name} />
                  <div className="place-card-overlay" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: '8px' }}>
                    <div className="place-card-title" style={{ marginBottom: '8px', textShadow: '0 1px 3px rgba(0,0,0,0.8)', color: 'white' }}>{place.name}</div>
                    <div style={{ display: 'flex', gap: '6px', pointerEvents: 'auto' }}>
                      <button 
                        onClick={(e) => { e.stopPropagation(); openRoute(place); }}
                        style={{ flex: 1, padding: '6px 4px', fontSize: '0.75rem', fontWeight: 'bold', background: 'rgba(255,255,255,0.25)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.4)', borderRadius: '6px', color: 'white', cursor: 'pointer' }}
                      >
                        Google Map
                      </button>
                      <button 
                        onClick={(e) => { e.stopPropagation(); openInRunningApp(place); }}
                        style={{ flex: 1, padding: '6px 4px', fontSize: '0.75rem', fontWeight: 'bold', background: 'rgba(255,255,255,0.25)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.4)', borderRadius: '6px', color: 'white', cursor: 'pointer' }}
                      >
                        Running App
                      </button>
                    </div>
                  </div>
                  {place.rating && (
                    <div className="place-card-rating">
                      ⭐ {place.rating}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Search Modal */}
      {showSearch && (
        <div className="modal-overlay" onClick={() => setShowSearch(false)}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()}>
            <h2>キーワードで調べる</h2>
            <input 
              type="text" 
              className="input-field" 
              placeholder="例: パスタ、歴史的建造物..." 
              value={searchKeyword}
              onChange={e => setSearchKeyword(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleSearchSubmit(); }}
              autoFocus
            />
            <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
              <button className="btn-primary" style={{ flex: 1 }} onClick={handleSearchSubmit}>検索</button>
              <button className="glass-pill" style={{ flex: 1, textAlign: 'center' }} onClick={() => setShowSearch(false)}>キャンセル</button>
            </div>
          </div>
        </div>
      )}

      {/* Settings Modal */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()}>
            <h2>設定</h2>
            
            <div style={{ marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '8px', color: 'var(--text-secondary)' }}>テーマ（見た目）</h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className={`glass-pill ${!isDarkMode ? 'active' : ''}`} onClick={() => setIsDarkMode(false)}>ライト</button>
                <button className={`glass-pill ${isDarkMode ? 'active' : ''}`} onClick={() => setIsDarkMode(true)}>ダーク</button>
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '8px', color: 'var(--text-secondary)' }}>新しいタグを追加</h3>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                <input 
                  type="text" 
                  className="input-field" 
                  style={{ flex: '1 1 200px' }}
                  placeholder="例: パン屋、映えスポット" 
                  value={newTagName}
                  onChange={e => setNewTagName(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter') handleAddTag(); }}
                />
                <button className="btn-primary" onClick={handleAddTag}>追加</button>
              </div>
            </div>

            <button className="glass-pill" style={{ width: '100%', textAlign: 'center', marginTop: '8px' }} onClick={() => setShowSettings(false)}>閉じる</button>
          </div>
        </div>
      )}
    </>
  );
}

export default App;
