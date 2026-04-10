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

const CATEGORIES = [
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
  const [selectedCategory, setSelectedCategory] = useState(CATEGORIES[0]);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState(null);
  const [sheetExpanded, setSheetExpanded] = useState(false);
  const [startY, setStartY] = useState(0);

  const handlePointerDown = (e) => {
    setStartY(e.clientY);
    e.target.setPointerCapture(e.pointerId);
  };

  const handlePointerUp = (e) => {
    const deltaY = e.clientY - startY;
    e.target.releasePointerCapture(e.pointerId);

    if (deltaY < -30) {
      setSheetExpanded(true); // Dragged up
    } else if (deltaY > 30) {
      setSheetExpanded(false); // Dragged down
    } else if (Math.abs(deltaY) < 5) {
      setSheetExpanded(!sheetExpanded); // Simple click
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
      keyword: selectedCategory.searchContext
    };

    service.nearbySearch(request, (results, status) => {
      setIsSearching(false);
      if (status === window.google.maps.places.PlacesServiceStatus.OK && results) {
        // Filter places with photos
        const withPhotos = results.filter(p => p.photos && p.photos.length > 0);
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

  // Handle map drag end to re-search
  const handleDragEnd = () => {
    if (map) {
      const newCenter = map.getCenter();
      setCenter({ lat: newCenter.lat(), lng: newCenter.lng() });
    }
  };

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
    
    // Render（本番環境）などでは環境変数でホストを指定、ローカル時は現在のIPを使う
    let baseUrl = import.meta.env.VITE_RUNNING_APP_URL || `https://${window.location.hostname}:5000`;
    // 末尾のスラッシュは除去してからクエリをつなげる
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
            styles: [ // Dark mode map
              { elementType: 'geometry', stylers: [{ color: '#242f3e' }] },
              { elementType: 'labels.text.stroke', stylers: [{ color: '#242f3e' }] },
              { elementType: 'labels.text.fill', stylers: [{ color: '#746855' }] },
              {
                featureType: 'water',
                elementType: 'geometry',
                stylers: [{ color: '#17263c' }]
              }
            ]
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
            <Search size={20} color="white" style={{ cursor: 'pointer' }} onClick={() => alert('検索機能は準備中です')} />
            <Settings size={20} color="white" style={{ cursor: 'pointer' }} onClick={() => alert('設定画面は準備中です')} />
          </div>
        </div>

        <div className="category-scroll top-bar-content">
          {CATEGORIES.map(cat => (
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
          <button className="glass-pill" style={{ background: 'var(--primary-color)' }} onClick={fetchPlaces}>
            このエリアを検索
          </button>
        </div>
      </div>

      {/* Bottom Sheet */}
      <div className={`bottom-sheet ${sheetExpanded ? 'expanded' : ''}`}>
        <div 
          className="bottom-sheet-handle" 
          onPointerDown={handlePointerDown}
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
            <div className="center-flex text-secondary" style={{ height: '200px' }}>写真のある場所が見つかりませんでした。</div>
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
                    <div className="place-card-title" style={{ marginBottom: '8px', textShadow: '0 1px 3px rgba(0,0,0,0.8)' }}>{place.name}</div>
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
    </>
  );
}

export default App;
