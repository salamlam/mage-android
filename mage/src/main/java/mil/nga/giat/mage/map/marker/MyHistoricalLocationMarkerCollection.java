package mil.nga.giat.mage.map.marker;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.MinMaxPriorityQueue;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;

/**
 * Class uses a queue like structure to limit the Collection size. Size determined 
 * by LocationPushIntentService.minNumberOfLocationsToKeep
 * 
 * @author wiedemanns
 * 
 */
public class MyHistoricalLocationMarkerCollection extends LocationMarkerCollection {

	// use a queue like structure to limit the Collection size
	protected MinMaxPriorityQueue<Location> locationQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Location>() {
		@Override
		public int compare(Location lhs, Location rhs) {
			return lhs.getTimestamp().compareTo(rhs.getTimestamp());
		}
	}).expectedSize(LocationPushIntentService.minNumberOfLocationsToKeep).create();

	public MyHistoricalLocationMarkerCollection(Context context, GoogleMap map) {
		super(context, map);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		Location l = markerIdToLocation.get(marker.getId());
        return (l != null);
	}

	@Override
	public void add(Location l) {
		final Geometry g = l.getGeometry();
		if (g != null) {
			// If I got an observation that I already have in my list
			// remove it from the map and clean-up my collections
			Marker marker = locationIdToMarker.remove(l.getId());
			if (marker != null) {
				markerIdToLocation.remove(marker.getId());
				marker.remove();
			}

			Point point = g.getCentroid();
			MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).icon(LocationBitmapFactory.dotBitmapDescriptor(context, l, l.getUser())).visible(visible);

			marker = markerCollection.addMarker(options);

			locationIdToMarker.put(l.getId(), marker);
			markerIdToLocation.put(marker.getId(), l);
			locationQueue.add(l);

			while (locationQueue.size() > LocationPushIntentService.minNumberOfLocationsToKeep) {
				remove(locationQueue.poll());
			}
			
			removeOldMarkers();
		}
	}
	
	@Override
	public Date getLatestDate() {
		return locationQueue.peekLast().getTimestamp();
	}

	@Override
	public void refreshMarkerIcons() {
		for (Marker m : markerCollection.getMarkers()) {
			Location tl = markerIdToLocation.get(m.getId());
			if (tl != null) {
				boolean showWindow = m.isInfoWindowShown();
				// make sure to set the Anchor after this call as well, because the size of the icon might have changed
				m.setIcon(LocationBitmapFactory.dotBitmapDescriptor(context, tl, tl.getUser()));
				m.setAnchor(0.5f, 1.0f);
				if (showWindow) {
					m.showInfoWindow();
				}
			}
		}
	}

	/**
	 * Used to remove markers for locations that have been removed from the local datastore.
	 */
	@Override
	public void removeOldMarkers() {
		LocationHelper lh = LocationHelper.getInstance(context.getApplicationContext());
		Set<Long> locationIds = locationIdToMarker.keySet();
		for (Long locationId : locationIds) {
			Location locationExists = new Location();
			locationExists.setId(locationId);
			if (!lh.exists(locationExists)) {
				Marker marker = locationIdToMarker.remove(locationId);
				if (marker != null) {
					Location l = markerIdToLocation.remove(marker.getId());
					locationQueue.remove(l);
					marker.remove();
				}
			}
		}
	}

	@Override
	public void clear() {
		super.clear();
		locationQueue.clear();
	}
}
