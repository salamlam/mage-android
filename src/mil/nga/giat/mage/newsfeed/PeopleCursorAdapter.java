package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.ocpsoft.prettytime.PrettyTime;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

public class PeopleCursorAdapter extends CursorAdapter {
	private static final String LOG_NAME = PeopleCursorAdapter.class.getName();

	private static final String ASSET = "people/person.png";
	private static final String DEFAULT_ASSET = "people/high/person.png";
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Location> query;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.ENGLISH);

	public PeopleCursorAdapter(Context context, Cursor c, PreparedQuery<Location> query) {
		super(context, c, false);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.query = query;
	}

	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		try {
			Location location = query.mapRow(new AndroidDatabaseResults(cursor, null));
			User user = location.getUser();

			ImageView iconView = (ImageView) v.findViewById(R.id.iconImageView);
			Bitmap iconMarker = LocationBitmapFactory.bitmap(context, location, ASSET, DEFAULT_ASSET);
			if (iconMarker != null) {
				iconView.setImageBitmap(iconMarker);
			}

			TextView location_name = (TextView) v.findViewById(R.id.location_name);
			location_name.setText(user.getFirstname() + " " + user.getLastname());

			TextView location_email = (TextView) v.findViewById(R.id.location_email);
			String email = user.getEmail();
			if (email != null && !email.trim().isEmpty()) {
				location_email.setVisibility(View.VISIBLE);
				location_email.setText(email);
			} else {
				location_email.setVisibility(View.GONE);
			}
			
			// set date
			TextView location_date = (TextView) v.findViewById(R.id.location_date);

			String timeText = sdf.format(location.getTimestamp());
			Boolean prettyPrint = PreferenceHelper.getInstance(context).getValue(R.string.prettyPrintLocationDatesKey, Boolean.class, R.string.prettyPrintLocationDatesDefaultValue);
			if(prettyPrint) {
				//timeText = DateUtils.getRelativeTimeSpanString(location.getTimestamp().getTime(), System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_ALL).toString();
				timeText = new PrettyTime().format(location.getTimestamp());
			}

			location_date.setText(timeText);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Could not set location view informaiton.", sqle);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return inflater.inflate(R.layout.people_list_item, parentView, false);
	}
}