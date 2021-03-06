package mil.nga.giat.mage.map;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageCache;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.geom.map.GoogleMapShapeConverter;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.projection.ProjectionTransform;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.MapFeatureTiles;
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile;
import mil.nga.geopackage.tiles.overlay.BoundedOverlay;
import mil.nga.geopackage.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventBannerFragment;
import mil.nga.giat.mage.filter.DateTimeFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayType;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay;
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay;
import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.map.marker.MyHistoricalLocationMarkerCollection;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.wkb.geom.GeometryType;

public class MapFragment extends Fragment implements OnMapReadyCallback, OnMapClickListener, OnMapLongClickListener, OnMarkerClickListener, OnInfoWindowClickListener, OnMapPanListener, OnMyLocationButtonClickListener, OnClickListener, LocationSource, LocationListener, OnCacheOverlayListener, OnSharedPreferenceChangeListener,
		IObservationEventListener, ILocationEventListener, IStaticFeatureEventListener {

	private static final String LOG_NAME = MapFragment.class.getName();

	private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

	private MAGE mage;
	private MapView mapView;
	private GoogleMap map;
	private boolean mapInitialized = false;
	private View searchLayout;
	private EditText edittextSearch;
	private Location location;
	private boolean followMe = false;
	private GoogleMapWrapper mapWrapper;
	protected User currentUser = null;
	private OnLocationChangedListener locationChangedListener;

	private static final int REFRESHMARKERINTERVALINSECONDS = 30;
	private RefreshMarkersTask refreshLocationsMarkersTask;
	private RefreshMarkersTask refreshMyHistoricLocationsMarkersTask;

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(64);
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 10, TimeUnit.SECONDS, queue);

	private PointCollection<Observation> observations;
	private PointCollection<mil.nga.giat.mage.sdk.datastore.location.Location> locations;
	private PointCollection<mil.nga.giat.mage.sdk.datastore.location.Location> historicLocations;
	private StaticGeometryCollection staticGeometryCollection;
	private List<Marker> searchMarkers = new ArrayList<>();

	private Map<String, CacheOverlay> cacheOverlays = new HashMap<>();

	// GeoPackage cache of open GeoPackage connections
	private GeoPackageCache geoPackageCache;
	private BoundingBox addedCacheBoundingBox;

	private LocationService locationService;

	SharedPreferences preferences;

	EventBannerFragment eventBannerFragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_map, container, false);

		FragmentManager fragmentManager = getChildFragmentManager();
		eventBannerFragment = new EventBannerFragment();
		fragmentManager.beginTransaction().add(R.id.map_event_holder, eventBannerFragment).commit();

		setHasOptionsMenu(true);

		mage = (MAGE) getActivity().getApplication();
		locationService = mage.getLocationService();

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

		searchLayout = view.findViewById(R.id.search_layout);
		edittextSearch = (EditText) view.findViewById(R.id.edittext_search);

		MapsInitializer.initialize(getActivity().getApplicationContext());

		ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
		mapSettings.setOnClickListener(this);

		mapWrapper = new GoogleMapWrapper(getActivity().getApplicationContext());
		mapWrapper.addView(view);

		mapView = (MapView) view.findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		// Initialize the GeoPackage cache with a GeoPackage manager
		GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(getActivity().getApplicationContext());
		geoPackageCache = new GeoPackageCache(geoPackageManager);

		return mapWrapper;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mapView.onDestroy();

		ObservationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);
		LocationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);


		if (observations != null) {
			observations.clear();
			observations = null;
		}

		if (locations != null) {
			locations.clear();
			locations = null;
		}

		if (historicLocations != null) {
			historicLocations.clear();
			historicLocations = null;
		}

		if (searchMarkers != null) {
			for (Marker m : searchMarkers) {
				m.remove();
			}
			searchMarkers.clear();
		}

		// Close all open GeoPackages
		geoPackageCache.closeAll();

		staticGeometryCollection = null;
		currentUser = null;
		map = null;
		mapInitialized = false;
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
		initializeMap();
	}

	private void initializeMap() {
		if (map == null) return;

		if (!mapInitialized) {
			mapInitialized = true;

			map.setOnMapClickListener(this);
			map.setOnMarkerClickListener(this);
			map.setOnMapLongClickListener(this);
			map.setOnMyLocationButtonClickListener(this);
			map.setOnInfoWindowClickListener(this);

			staticGeometryCollection = new StaticGeometryCollection();

			locations = new LocationMarkerCollection(getActivity(), map);
			LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
			locationLoad.setFilter(getTemporalFilter("timestamp"));
			locationLoad.executeOnExecutor(executor);

			historicLocations = new MyHistoricalLocationMarkerCollection(getActivity(), map);
			HistoricLocationLoadTask myHistoricLocationLoad = new HistoricLocationLoadTask(getActivity(), historicLocations);
			myHistoricLocationLoad.executeOnExecutor(executor);

			ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
			LocationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
		}

		if (observations != null) {
			observations.clear();
		}

		observations = new ObservationMarkerCollection(getActivity(), map);
		ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
		observationLoad.addFilter(getTemporalFilter("last_modified"));
		observationLoad.executeOnExecutor(executor);

		updateMapView();

		// Set visibility on map markers as preferences may have changed
		observations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showObservationsKey), true));
		locations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showLocationsKey), true));
		historicLocations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showMyLocationHistoryKey), false));

		// task to refresh location markers every x seconds
		refreshLocationsMarkersTask = new RefreshMarkersTask(locations);
		if (!refreshLocationsMarkersTask.isCancelled()) {
			refreshLocationsMarkersTask.executeOnExecutor(executor, REFRESHMARKERINTERVALINSECONDS);
		}

		// task to refresh my historic location markers every x seconds
		refreshMyHistoricLocationsMarkersTask = new RefreshMarkersTask(historicLocations);
		if (!refreshMyHistoricLocationsMarkersTask.isCancelled()) {
			refreshMyHistoricLocationsMarkersTask.executeOnExecutor(executor, REFRESHMARKERINTERVALINSECONDS);
		}

		updateStaticFeatureLayers();

		CacheProvider.getInstance(getActivity().getApplicationContext()).registerCacheOverlayListener(this);
		StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).addListener(this);

		// Check if any map preferences changed that I care about
		if (locationService != null && ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			map.setMyLocationEnabled(true);
			map.setLocationSource(this);
			locationService.registerOnLocationListener(this);
		} else {
			map.setMyLocationEnabled(false);
			map.setLocationSource(null);
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	public void onResume() {
		super.onResume();

		try {
			currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Could not find current user.", ue);
		}

		mapView.onResume();
		initializeMap();

		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

		getActivity().getActionBar().setTitle(getTemporalFilterTitle());

		edittextSearch.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					search(v);
					return true;
				} else {
					return false;
				}
			}
		});
		
		edittextSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s == null || s.toString().trim().isEmpty()) {
					if (searchMarkers != null) {
						for (Marker m : searchMarkers) {
							m.remove();
						}
						searchMarkers.clear();
					}
				}
			}

		});
	}

	@Override
	public void onPause() {
		super.onPause();

		if (refreshLocationsMarkersTask != null) {
			refreshLocationsMarkersTask.cancel(true);
			refreshLocationsMarkersTask = null;
		}

		if (refreshMyHistoricLocationsMarkersTask != null) {
			refreshMyHistoricLocationsMarkersTask.cancel(true);
			refreshMyHistoricLocationsMarkersTask = null;
		}
		
		mapView.onPause();

		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		CacheProvider.getInstance(getActivity().getApplicationContext()).unregisterCacheOverlayListener(this);
		StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

		if (map != null) {
			saveMapView();

			map.setLocationSource(null);
			if (locationService != null) {
				locationService.unregisterOnLocationListener(this);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.search, menu);
		inflater.inflate(R.menu.observation_new, menu);
		inflater.inflate(R.menu.filter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.observation_new:
			onNewObservation();
			break;
		case R.id.search:
			boolean isVisible = searchLayout.getVisibility() == View.VISIBLE;
			searchLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);

			if (isVisible) {
				hideKeyboard();
			} else {
				edittextSearch.requestFocus();
				InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
				inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
			}

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void onNewObservation() {
		Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
		Location l = locationService.getLocation();

		// if there is not a location from the location service, then try to pull one from the database.
		if (l == null) {
			List<mil.nga.giat.mage.sdk.datastore.location.Location> tLocations = LocationHelper.getInstance(getActivity().getApplicationContext()).getCurrentUserLocations(getActivity().getApplicationContext(), 1, true);
			if (!tLocations.isEmpty()) {
				mil.nga.giat.mage.sdk.datastore.location.Location tLocation = tLocations.get(0);
				Geometry geo = tLocation.getGeometry();
				Map<String, LocationProperty> propertiesMap = tLocation.getPropertiesMap();
				if (geo instanceof Point) {
					Point point = (Point) geo;
					String provider = "manual";
					if (propertiesMap.get("provider").getValue() != null) {
						provider = propertiesMap.get("provider").getValue().toString();
					}
					l = new Location(provider);
					l.setTime(tLocation.getTimestamp().getTime());
					if (propertiesMap.get("accuracy").getValue() != null) {
						l.setAccuracy(Float.valueOf(propertiesMap.get("accuracy").getValue().toString()));
					}
					l.setLatitude(point.getY());
					l.setLongitude(point.getX());
				}
			}
		} else {
			l = new Location(l);
		}

		if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
				.setTitle(getActivity().getResources().getString(R.string.location_no_event_title))
				.setMessage(getActivity().getResources().getString(R.string.location_no_event_message))
				.setPositiveButton(android.R.string.ok, null)
				.show();
		} else if (l != null) {
			intent.putExtra(ObservationEditActivity.LOCATION, l);
			intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, map.getCameraPosition().target);
			intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
			startActivity(intent);
		} else {
			if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
						.setTitle(getActivity().getResources().getString(R.string.location_missing_title))
						.setMessage(getActivity().getResources().getString(R.string.location_missing_message))
						.setPositiveButton(android.R.string.ok, null)
						.show();
			} else {
				new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
						.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
						.setMessage(getActivity().getResources().getString(R.string.location_access_observation_message))
						.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								FragmentCompat.requestPermissions(MapFragment.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
							}
						})
						.show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					onNewObservation();
				}
				break;
			}
		}
	}

	@Override
	public void onObservationCreated(Collection<Observation> o, Boolean sendUserNotifcations) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(getActivity(), ObservationTask.Type.ADD, observations);
			task.addFilter(getTemporalFilter("last_modified"));
			task.executeOnExecutor(executor, o.toArray(new Observation[o.size()]));
		}
	}

	@Override
	public void onObservationUpdated(Observation o) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(getActivity(), ObservationTask.Type.UPDATE, observations);
			task.addFilter(getTemporalFilter("last_modified"));
			task.executeOnExecutor(executor, o);
		}
	}

	@Override
	public void onObservationDeleted(Observation o) {
		if (observations != null) {
			new ObservationTask(getActivity(), ObservationTask.Type.DELETE, observations).executeOnExecutor(executor, o);
		}
	}

	@Override
	public void onLocationCreated(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> ls) {
		for (mil.nga.giat.mage.sdk.datastore.location.Location l : ls) {
			if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
				if (locations != null) {
					LocationTask task = new LocationTask(LocationTask.Type.ADD, locations);
					task.setFilter(getTemporalFilter("timestamp"));
					task.executeOnExecutor(executor, l);
				}
			} else {
				if (historicLocations != null) {
					new LocationTask(LocationTask.Type.ADD, historicLocations).executeOnExecutor(executor, l);
				}
			}
		}
	}

	@Override
	public void onLocationUpdated(mil.nga.giat.mage.sdk.datastore.location.Location l) {
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
			if (locations != null) {
				LocationTask task = new LocationTask(LocationTask.Type.UPDATE, locations);
				task.setFilter(getTemporalFilter("timestamp"));
				task.executeOnExecutor(executor, l);
			}
		} else {
			if (historicLocations != null) {
				new LocationTask(LocationTask.Type.UPDATE, historicLocations).executeOnExecutor(executor, l);
			}
		}
	}

	@Override
	public void onLocationDeleted(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> l) {
		// this is slowing the app down a lot!  Moving the delete like code into the add methods of the collections
		/*
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
			if (locations != null) {
				new LocationTask(LocationTask.Type.DELETE, locations).execute(l);
			}
		} else {
			if (myHistoricLocations != null) {
				new LocationTask(LocationTask.Type.DELETE, myHistoricLocations).execute(l);
			}
		}
		*/
	}
	
	@Override
	public void onInfoWindowClick(Marker marker) {
		observations.onInfoWindowClick(marker);
		locations.onInfoWindowClick(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		hideKeyboard();
		// search marker
		if(searchMarkers != null) {
			for(Marker m :searchMarkers) {
				 if(marker.getId().equals(m.getId())) {
						m.showInfoWindow();
						return true;		 
				 }
			}
		}
		
		// You can only have one marker click listener per map.
		// Lets listen here and shell out the click event to all
		// my marker collections. Each one need to handle
		// gracefully if it does not actually contain the marker
		if (observations.onMarkerClick(marker)) {
			return true;
		}

		if (locations.onMarkerClick(marker)) {
			return true;
		}

		if (historicLocations.onMarkerClick(marker)) {
			return true;
		}

		// static layer
		if(marker.getSnippet() != null) {
			View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.static_feature_infowindow, null, false);
			WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.static_feature_infowindow_content));
			webView.loadData(marker.getSnippet(), "text/html; charset=UTF-8", null);
			new AlertDialog.Builder(getActivity())
				.setView(markerInfoWindow)
				.setPositiveButton(android.R.string.yes, null)
				.show();
		}
		return true;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		hideKeyboard();
		// remove old accuracy circle
		((LocationMarkerCollection) locations).offMarkerClick();

		staticGeometryCollection.onMapClick(map, latLng, getActivity());

		if(!cacheOverlays.isEmpty()) {
			StringBuilder clickMessage = new StringBuilder();
			for (CacheOverlay cacheOverlay : cacheOverlays.values()) {
				String message = cacheOverlay.onMapClick(latLng, mapView, map);
				if(message != null){
					if(clickMessage.length() > 0){
						clickMessage.append("\n\n");
					}
					clickMessage.append(message);
				}
			}
			if(clickMessage.length() > 0) {
				new AlertDialog.Builder(getActivity())
					.setMessage(clickMessage.toString())
					.setPositiveButton(android.R.string.yes, null)
					.show();
			}
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		hideKeyboard();
		if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
				.setTitle(getActivity().getResources().getString(R.string.location_no_event_title))
				.setMessage(getActivity().getResources().getString(R.string.location_no_event_message))
				.setPositiveButton(android.R.string.ok, null)
				.show();
		} else {
			Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
			Location l = new Location("manual");
			l.setAccuracy(0.0f);
			l.setLatitude(point.latitude);
			l.setLongitude(point.longitude);
			l.setTime(new Date().getTime());
			intent.putExtra(ObservationEditActivity.LOCATION, l);
			startActivity(intent);
		}
	}

	@Override
	public void onClick(View view) {
		// close keyboard
		hideKeyboard();
		switch (view.getId()) {
		case R.id.map_settings: {
			Intent i = new Intent(getActivity().getApplicationContext(), MapPreferencesActivity.class);
			startActivity(i);
			break;
		}
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		if (location != null) {
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			float zoom = map.getCameraPosition().zoom < 15 ? 15 : map.getCameraPosition().zoom;
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {

				@Override
				public void onCancel() {
					mapWrapper.setOnMapPanListener(MapFragment.this);
					followMe = true;
				}

				@Override
				public void onFinish() {
					mapWrapper.setOnMapPanListener(MapFragment.this);
					followMe = true;
				}
			});
		}
		return true;
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		Log.i(LOG_NAME, "map location, activate");
		locationChangedListener = listener;
		if (location != null) {
			Log.i(LOG_NAME, "map location, activate we have a location, let our listener know");
			locationChangedListener.onLocationChanged(location);
		}
	}

	@Override
	public void deactivate() {
		Log.i(LOG_NAME, "map location, deactivate");
		locationChangedListener = null;
	}

	@Override
	public void onMapPan() {
		mapWrapper.setOnMapPanListener(null);
		followMe = false;
	}

	@Override
	public void onLocationChanged(Location location) {
		this.location = location;
		Log.d(LOG_NAME, "Map location updated.");
		if (locationChangedListener != null) {
			locationChangedListener.onLocationChanged(location);
		}

		if (followMe) {
			LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			if (!bounds.contains(latLng)) {
				// Move the camera to the user's location once it's available!
				map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {

		// Add all overlays that are in the preferences

		// Track enabled cache overlays
		Map<String, CacheOverlay> enabledCacheOverlays = new HashMap<>();

		// Track enabled GeoPackages
		Set<String> enabledGeoPackages = new HashSet<>();

		// Reset the bounding box for newly added caches
		addedCacheBoundingBox = null;

		for (CacheOverlay cacheOverlay : cacheOverlays) {

			// If this cache overlay potentially replaced by a new version
			if(cacheOverlay.isAdded()){
				if(cacheOverlay.getType() == CacheOverlayType.GEOPACKAGE){
					geoPackageCache.close(cacheOverlay.getName());
				}
			}

			// The user has asked for this overlay
			if (cacheOverlay.isEnabled()) {

				// Handle each type of cache overlay
				switch(cacheOverlay.getType()) {

					case XYZ_DIRECTORY:
						addXYZDirectoryCacheOverlay(enabledCacheOverlays, (XYZDirectoryCacheOverlay) cacheOverlay);
						break;

					case GEOPACKAGE:
						addGeoPackageCacheOverlay(enabledCacheOverlays, enabledGeoPackages, (GeoPackageCacheOverlay)cacheOverlay);
						break;
				}
			}

			cacheOverlay.setAdded(false);
		}

		// Remove any overlays that are on the map but no longer selected in
		// preferences, update the tile overlays to the enabled tile overlays
		for (CacheOverlay cacheOverlay : this.cacheOverlays.values()) {
			cacheOverlay.removeFromMap();
		}
		this.cacheOverlays = enabledCacheOverlays;

		// Close GeoPackages no longer enabled
		geoPackageCache.closeRetain(enabledGeoPackages);

		// If a new cache was added, zoom to the bounding box area
		if(addedCacheBoundingBox != null){

			final LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMinLatitude(), addedCacheBoundingBox
					.getMinLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMinLatitude(), addedCacheBoundingBox
					.getMaxLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMaxLatitude(), addedCacheBoundingBox
					.getMinLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMaxLatitude(), addedCacheBoundingBox
					.getMaxLongitude()));

			try {
				map.animateCamera(CameraUpdateFactory.newLatLngBounds(
						boundsBuilder.build(), 0));
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to move camera to newly added cache location", e);
			}
		}
	}

	/**
	 * Add XYZ Directory tile cache overlay
	 * @param enabledCacheOverlays
	 * @param xyzDirectoryCacheOverlay
	 */
	private void addXYZDirectoryCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, XYZDirectoryCacheOverlay xyzDirectoryCacheOverlay){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(xyzDirectoryCacheOverlay.getCacheName());
		if(cacheOverlay == null){
			// Create a new tile provider and add to the map
			TileProvider tileProvider = new FileSystemTileProvider(256, 256, xyzDirectoryCacheOverlay.getDirectory().getAbsolutePath());
			TileOverlayOptions overlayOptions = createTileOverlayOptions(tileProvider);
			// Set the tile overlay in the cache overlay
			TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
			xyzDirectoryCacheOverlay.setTileOverlay(tileOverlay);
			cacheOverlay = xyzDirectoryCacheOverlay;
		}
		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Add a GeoPackage cache overlay, which contains tile and feature tables
	 * @param enabledCacheOverlays
	 * @param enabledGeoPackages
	 * @param geoPackageCacheOverlay
	 */
	private void addGeoPackageCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, Set<String> enabledGeoPackages, GeoPackageCacheOverlay geoPackageCacheOverlay){

		// Check each GeoPackage table
		for(CacheOverlay tableCacheOverlay: geoPackageCacheOverlay.getChildren()){
			// Check if the table is enabled
			if(tableCacheOverlay.isEnabled()){

				// Get and open if needed the GeoPackage
				GeoPackage geoPackage = geoPackageCache.getOrOpen(geoPackageCacheOverlay.getName());
				enabledGeoPackages.add(geoPackage.getName());

				// Handle tile and feature tables
				switch(tableCacheOverlay.getType()){
					case GEOPACKAGE_TILE_TABLE:
						addGeoPackageTileCacheOverlay(enabledCacheOverlays, (GeoPackageTileTableCacheOverlay)tableCacheOverlay, geoPackage, false);
						break;
					case GEOPACKAGE_FEATURE_TABLE:
						addGeoPackageFeatureCacheOverlay(enabledCacheOverlays, (GeoPackageFeatureTableCacheOverlay)tableCacheOverlay, geoPackage);
						break;
					default:
						throw new UnsupportedOperationException("Unsupported GeoPackage type: " + tableCacheOverlay.getType());
				}

				// If a newly added cache, update the bounding box for zooming
				if(geoPackageCacheOverlay.isAdded()){

					try {
						ContentsDao contentsDao = geoPackage.getContentsDao();
						Contents contents = contentsDao.queryForId(tableCacheOverlay.getName());
						BoundingBox contentsBoundingBox = contents.getBoundingBox();
						Projection projection = ProjectionFactory
								.getProjection(contents.getSrs().getOrganizationCoordsysId());

						ProjectionTransform transform = projection.getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);
						BoundingBox boundingBox = transform.transform(contentsBoundingBox);
						boundingBox = TileBoundingBoxUtils.boundWgs84BoundingBoxWithWebMercatorLimits(boundingBox);

						if (addedCacheBoundingBox == null) {
							addedCacheBoundingBox = boundingBox;
						} else {
							addedCacheBoundingBox = TileBoundingBoxUtils.union(addedCacheBoundingBox, boundingBox);
						}
					}catch(Exception e){
						Log.e(LOG_NAME, "Failed to retrieve GeoPackage Table bounding box. GeoPackage: "
								+ geoPackage.getName() + ", Table: " + tableCacheOverlay.getName(), e);
					}
				}
			}
		}
	}

	/**
	 * Add the GeoPackage Tile Table Cache Overlay
	 * @param enabledCacheOverlays
	 * @param tileTableCacheOverlay
	 * @param geoPackage
	 * @param linkedToFeatures
	 */
	private void addGeoPackageTileCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, GeoPackageTileTableCacheOverlay tileTableCacheOverlay, GeoPackage geoPackage, boolean linkedToFeatures){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(tileTableCacheOverlay.getCacheName());
		if(cacheOverlay != null){
			// If the existing cache overlay is being replaced, create a new cache overlay
			if(tileTableCacheOverlay.getParent().isAdded()){
				cacheOverlay = null;
			}
		}
		if(cacheOverlay == null){
			// Create a new GeoPackage tile provider and add to the map
			TileDao tileDao = geoPackage.getTileDao(tileTableCacheOverlay.getName());
			BoundedOverlay geoPackageTileProvider = GeoPackageOverlayFactory.getBoundedOverlay(tileDao);
			TileOverlayOptions overlayOptions = null;
			if(linkedToFeatures){
				overlayOptions = createFeatureTileOverlayOptions(geoPackageTileProvider);
			}else {
				overlayOptions = createTileOverlayOptions(geoPackageTileProvider);
			}
			TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
			tileTableCacheOverlay.setTileOverlay(tileOverlay);

			// Check for linked feature tables
			tileTableCacheOverlay.clearFeatureOverlayQueries();
			FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
			List<FeatureDao> featureDaos = linker.getFeatureDaosForTileTable(tileDao.getTableName());
			for(FeatureDao featureDao: featureDaos){

				// Create the feature tiles
				FeatureTiles featureTiles = new MapFeatureTiles(getActivity(), featureDao);

				// Create an index manager
				FeatureIndexManager indexer = new FeatureIndexManager(getActivity(), geoPackage, featureDao);
				featureTiles.setIndexManager(indexer);

				// Add the feature overlay query
				FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(getActivity(), geoPackageTileProvider, featureTiles);
				tileTableCacheOverlay.addFeatureOverlayQuery(featureOverlayQuery);
			}

			cacheOverlay = tileTableCacheOverlay;
		}
		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Add the GeoPackage Feature Table Cache Overlay
	 * @param enabledCacheOverlays
	 * @param featureTableCacheOverlay
	 * @param geoPackage
	 */
	private void addGeoPackageFeatureCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, GeoPackageFeatureTableCacheOverlay featureTableCacheOverlay, GeoPackage geoPackage){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(featureTableCacheOverlay.getCacheName());
		if(cacheOverlay != null){
			// If the existing cache overlay is being replaced, create a new cache overlay
			if(featureTableCacheOverlay.getParent().isAdded()){
				cacheOverlay = null;
			}
			for(GeoPackageTileTableCacheOverlay linkedTileTable: featureTableCacheOverlay.getLinkedTileTables()){
				if(cacheOverlay != null){
					// Add the existing linked tile cache overlays
					addGeoPackageTileCacheOverlay(enabledCacheOverlays, linkedTileTable, geoPackage, true);
				}
				cacheOverlays.remove(linkedTileTable.getCacheName());
			}
		}
		if(cacheOverlay == null) {
			// Add the features to the map
			FeatureDao featureDao = geoPackage.getFeatureDao(featureTableCacheOverlay.getName());

			// If indexed, add as a tile overlay
			if(featureTableCacheOverlay.isIndexed()){
				FeatureTiles featureTiles = new MapFeatureTiles(getActivity(), featureDao);
				Integer maxFeaturesPerTile = null;
				if(featureDao.getGeometryType() == GeometryType.POINT){
					maxFeaturesPerTile = getResources().getInteger(R.integer.geopackage_feature_tiles_max_points_per_tile);
				}else{
					maxFeaturesPerTile = getResources().getInteger(R.integer.geopackage_feature_tiles_max_features_per_tile);
				}
				featureTiles.setMaxFeaturesPerTile(maxFeaturesPerTile);
				NumberFeaturesTile numberFeaturesTile = new NumberFeaturesTile(getActivity());
				// Adjust the max features number tile draw paint attributes here as needed to
				// change how tiles are drawn when more than the max features exist in a tile
				featureTiles.setMaxFeaturesTileDraw(numberFeaturesTile);
				featureTiles.setIndexManager(new FeatureIndexManager(getActivity(), geoPackage, featureDao));
				// Adjust the feature tiles draw paint attributes here as needed to change how
				// features are drawn on tiles
				FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
				featureOverlay.setMinZoom(featureTableCacheOverlay.getMinZoom());

				FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
				List<TileDao> tileDaos = linker.getTileDaosForFeatureTable(featureDao.getTableName());
				featureOverlay.ignoreTileDaos(tileDaos);

				FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(getActivity(), featureOverlay);
				featureTableCacheOverlay.setFeatureOverlayQuery(featureOverlayQuery);
				TileOverlayOptions overlayOptions = createFeatureTileOverlayOptions(featureOverlay);
				TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
				featureTableCacheOverlay.setTileOverlay(tileOverlay);
			}
			// Not indexed, add the features to the map
			else {
				int maxFeaturesPerTable = 0;
				if(featureDao.getGeometryType() == GeometryType.POINT){
					maxFeaturesPerTable = getResources().getInteger(R.integer.geopackage_features_max_points_per_table);
				}else{
					maxFeaturesPerTable = getResources().getInteger(R.integer.geopackage_features_max_features_per_table);
				}
				Projection projection = featureDao.getProjection();
				GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter(projection);
				FeatureCursor featureCursor = featureDao.queryForAll();
				try {
					final int totalCount = featureCursor.getCount();
					int count = 0;
					while (featureCursor.moveToNext()) {
						FeatureRow featureRow = featureCursor.getRow();
						GeoPackageGeometryData geometryData = featureRow.getGeometry();
						if (geometryData != null && !geometryData.isEmpty()) {
							mil.nga.wkb.geom.Geometry geometry = geometryData.getGeometry();
							if (geometry != null) {
								GoogleMapShape shape = shapeConverter.toShape(geometry);
								// Set the Shape Marker, PolylineOptions, and PolygonOptions here if needed to change color and style
								featureTableCacheOverlay.addShapeToMap(featureRow.getId(), shape, map);

								if(++count >= maxFeaturesPerTable){
									if(count < totalCount){
										Toast.makeText(getActivity().getApplicationContext(), featureTableCacheOverlay.getCacheName()
												+ "- added " + count + " of " + totalCount, Toast.LENGTH_LONG).show();
									}
									break;
								}
							}
						}
					}
				} finally {
					featureCursor.close();
				}
			}

			// Add linked tile tables
			for(GeoPackageTileTableCacheOverlay linkedTileTable: featureTableCacheOverlay.getLinkedTileTables()){
				addGeoPackageTileCacheOverlay(enabledCacheOverlays, linkedTileTable, geoPackage, true);
			}

			cacheOverlay = featureTableCacheOverlay;
		}

		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Create Feature Tile Overlay Options with the default z index for tile layers drawn from features
	 * @param tileProvider
	 * @return
	 */
	private TileOverlayOptions createFeatureTileOverlayOptions(TileProvider tileProvider){
		return createTileOverlayOptions(tileProvider, -1);
	}

	/**
	 * Create Tile Overlay Options with the default z index for tile layers
	 * @param tileProvider
	 * @return
	 */
	private TileOverlayOptions createTileOverlayOptions(TileProvider tileProvider){
		return createTileOverlayOptions(tileProvider, -2);
	}

	/**
	 * Create Tile Overlay Options for the Tile Provider using the z index
	 * @param tileProvider
	 * @param zIndex
	 * @return
	 */
	private TileOverlayOptions createTileOverlayOptions(TileProvider tileProvider, int zIndex){
		TileOverlayOptions overlayOptions = new TileOverlayOptions();
		overlayOptions.tileProvider(tileProvider);
		overlayOptions.zIndex(zIndex);
		return overlayOptions;
	}

	private void updateStaticFeatureLayers() {
		removeStaticFeatureLayers();

		try {
			for (Layer l : LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent())) {
				onStaticFeatureLayer(l);
			}
		} catch (LayerException e) {
			Log.e(LOG_NAME, "Problem updating static features.", e);
		}
	}

	private void removeStaticFeatureLayers() {
		Set<String> selectedLayerIds = preferences.getStringSet(getResources().getString(R.string.staticFeatureLayersKey), Collections.<String> emptySet());

		for (String currentLayerId : staticGeometryCollection.getLayers()) {
			if (!selectedLayerIds.contains(currentLayerId)) {
				staticGeometryCollection.removeLayer(currentLayerId);
			}
		}
	}

	@Override
	public void onStaticFeaturesCreated(final Layer layer) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				onStaticFeatureLayer(layer);
			}
		});
	}

	private void onStaticFeatureLayer(Layer layer) {
		Set<String> layers = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getStringSet(getString(R.string.staticFeatureLayersKey), Collections.<String> emptySet());

		// The user has asked for this feature layer
		String layerId = layer.getId().toString();
		if (layers.contains(layerId) && layer.isLoaded()) {
			new StaticFeatureLoadTask(getActivity().getApplicationContext(), staticGeometryCollection, map).executeOnExecutor(executor, layer);
		}
	}

	private void updateMapView() {
		// Check the map type
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		// Check the map location and zoom
		String xyz = preferences.getString(getString(R.string.recentMapXYZKey), getString(R.string.recentMapXYZDefaultValue));
		if (xyz != null) {
			String[] values = StringUtils.split(xyz, ",");
			LatLng latLng = new LatLng(0.0, 0.0);
			if(values.length > 1) {
				try {
					latLng = new LatLng(Double.valueOf(values[1]), Double.valueOf(values[0]));
				} catch (NumberFormatException nfe) {
					Log.e(LOG_NAME, "Could not parse lon,lat: " + String.valueOf(values[1]) + ", " + String.valueOf(values[0]));
				}
			}
			float zoom = 1.0f;
			if(values.length > 2) {
				try {
					zoom = Float.valueOf(values[2]);
				} catch (NumberFormatException nfe) {
					Log.e(LOG_NAME, "Could not parse zoom level: " + String.valueOf(values[2]));
				}
			}
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		}
	}

	private void saveMapView() {
		CameraPosition position = map.getCameraPosition();

		String xyz = new StringBuilder().append(Double.valueOf(position.target.longitude).toString()).append(",").append(Double.valueOf(position.target.latitude).toString()).append(",").append(Float.valueOf(position.zoom).toString()).toString();
		preferences.edit().putString(getResources().getString(R.string.recentMapXYZKey), xyz).commit();
	}

	private void search(View v) {
		String searchString = edittextSearch.getText().toString();
		if (searchString != null && !searchString.equals("")) {

			InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (getActivity().getCurrentFocus() != null) {
				inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
			}

			new GeocoderTask(getActivity(), map, searchMarkers).execute(searchString);
		}
	}

	@Override
	public void onError(Throwable error) {
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (getResources().getString(R.string.activeTimeFilterKey).equalsIgnoreCase(key)) {
			observations.clear();
			ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
			observationLoad.addFilter(getTemporalFilter("timestamp"));
			observationLoad.executeOnExecutor(executor);

			locations.clear();
			LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
			locationLoad.setFilter(getTemporalFilter("timestamp"));
			locationLoad.executeOnExecutor(executor);

			getActivity().getActionBar().setTitle(getTemporalFilterTitle());
		} else if (getResources().getString(R.string.activeFavoritesFilterKey).equalsIgnoreCase(key) ||
				   getResources().getString(R.string.activeImportantFilterKey).equalsIgnoreCase(key)) {
			observations.clear();
			ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
			observationLoad.addFilter(getTemporalFilter("timestamp"));
			observationLoad.executeOnExecutor(executor);

			getActivity().getActionBar().setTitle(getTemporalFilterTitle());
			eventBannerFragment.refresh();
		}
	}

	private Filter<Temporal> getTemporalFilter(String columnName) {
		int[] timeFilterValues = getResources().getIntArray(R.array.timeFilterValues);
		int timeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), timeFilterValues[0]);

		Filter<Temporal> filter = null;

		Calendar c = Calendar.getInstance();
		switch (timeFilter) {
		case 4:
			c.add(Calendar.MONTH, -1);
			break;
		case 3:
			c.add(Calendar.DAY_OF_WEEK, -7);
			break;
		case 2:
			c.add(Calendar.HOUR, -24);
			break;
		case 1:
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			break;
		default:
			// no filter
			c = null;
		}

		if (c != null) {
			Date start = c.getTime();
			Date end = null;

			filter = new DateTimeFilter(start, end, columnName);
		}

		return filter;
	}

	private String getTemporalFilterTitle() {
		String title = "MAGE";
		int[] timeFilterValues = getResources().getIntArray(R.array.timeFilterValues);
		int timeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), timeFilterValues[0]);
		switch (timeFilter) {
		case 4:
			title = "Last Month";
			break;
		case 3:
			title = "Last Week";
			break;
		case 2:
			title = "Last 24 Hours";
			break;
		case 1:
			title = "Since Midnight";
			break;
		default:
			break;
		}

		return title;
	}

	private void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getActivity().getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
		}
	}
}