package mil.nga.giat.mage.profile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.giat.mage.sdk.profile.UpdateProfileTask;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class MyProfileFragment extends Fragment {

	private static final String LOG_NAME = MyProfileFragment.class.getName();
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";
	public static String USER_ID = "USER_ID";
	
	private Uri currentMediaUri;
	private User user;
	
	private MapView mapView;
	
	@Override
	public void onDestroy() {
		mapView.onDestroy();
		super.onDestroy();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
		getActivity().getActionBar().setTitle("My Profile");
		String userToLoad = getActivity().getIntent().getStringExtra(USER_ID);
		try {
			if (userToLoad != null) {
				user = UserHelper.getInstance(getActivity().getApplicationContext()).read(userToLoad);
			} else {
				user = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
			}
			
		} catch (UserException e) {
			e.printStackTrace();
		}
		final Long userId = user.getId();
		
		mapView = (MapView) rootView.findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		MapsInitializer.initialize(getActivity().getApplicationContext());
		
		LatLng latLng = getActivity().getIntent().getParcelableExtra(INITIAL_LOCATION);
		if (latLng == null) {
			latLng = new LatLng(0, 0);
		}
		float zoom = getActivity().getIntent().getFloatExtra(INITIAL_ZOOM, 0);
		mapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		List<Location> lastLocation = LocationHelper.getInstance(getActivity()).getUserLocations(userId, getActivity(), 1, true);
		
		LatLng location = new LatLng(0,0);
		
		if (!lastLocation.isEmpty()) {
			Geometry geo = lastLocation.get(0).getGeometry();
			if (geo instanceof Point) {
				Point point = (Point) geo;
				location = new LatLng(point.getY(), point.getX());
				MarkerOptions options = new MarkerOptions().position(location).visible(true);
				
				Marker marker = mapView.getMap().addMarker(options);
				marker.setIcon(LocationBitmapFactory.bitmapDescriptor(getActivity(), lastLocation.get(0), lastLocation.get(0).getUser()));
				mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
			}
		}
		
		TextView realName = (TextView)rootView.findViewById(R.id.realName);
		TextView username = (TextView)rootView.findViewById(R.id.username);
		TextView phone = (TextView)rootView.findViewById(R.id.phone);
		TextView email = (TextView)rootView.findViewById(R.id.email);
		
		realName.setText(user.getFirstname() + " " + user.getLastname());
		username.setText("(" + user.getUsername() + ")");
		if (user.getPrimaryPhone() == null) {
			phone.setVisibility(View.GONE);
		} else {
			phone.setVisibility(View.VISIBLE);
			phone.setText(user.getPrimaryPhone());
		}
		
		if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
			email.setVisibility(View.VISIBLE);
			email.setText(user.getEmail());
		} else {
			email.setVisibility(View.GONE);
		}
		final ImageView imageView = (ImageView)rootView.findViewById(R.id.profile_picture);
		String avatarUrl = user.getAvatarUrl();
		String localAvatarPath = user.getLocalAvatarPath();

		if(StringUtils.isNotBlank(localAvatarPath)) {
			File f = new File(localAvatarPath);
			setProfilePicture(f, imageView);
		} else {
			if (avatarUrl != null) {
				String localFilePath = MediaUtility.getAvatarDirectory() + "/" + user.getId() + ".png";

				DownloadImageTask avatarImageTask = new DownloadImageTask(getActivity().getApplicationContext(), Collections.singletonList(avatarUrl), Collections.singletonList(localFilePath), false) {

					@Override
					protected Void doInBackground(Void... v) {
						Void result = super.doInBackground(v);
						if(!errors.get(0)) {
							String lap = localFilePaths.get(0);
							user.setLocalAvatarPath(lap);
							try {
								UserHelper.getInstance(getActivity().getApplicationContext()).update(user);
							} catch (Exception e) {
								Log.e(LOG_NAME, e.getMessage(), e);
							}
							File f = new File(user.getLocalAvatarPath());
							setProfilePicture(f, imageView);
						}
						return result;
					}
				};
				avatarImageTask.execute();
			}
		}
		
		final Intent intent = new Intent(getActivity().getApplicationContext(), ProfilePictureViewerActivity.class);
		intent.putExtra(ProfilePictureViewerActivity.USER_ID, user.getId());
		
		rootView.findViewById(R.id.profile_picture).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					if (userId.equals(UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser().getId())) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					    builder.setItems(R.array.profileImageChoices, new DialogInterface.OnClickListener() {
						   public void onClick(DialogInterface dialog, int which) {
							   switch (which) {
							   case 0:
									startActivityForResult(intent, 1);
									break;
							   case 1:
								   // change the picture from the gallery
								   Intent intent = new Intent();
									intent.setType("image/*");
									intent.setAction(Intent.ACTION_GET_CONTENT);
									if (Build.VERSION.SDK_INT >= 18) {
										intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
									}
									startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
								   break;
							   case 2:
								   // change the picture from the camera
								   Intent caputreIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
									File f = null;
							        try {
							        	f = MediaUtility.createImageFile();
							        } catch (IOException ex) {
							            // Error occurred while creating the File
							        	ex.printStackTrace();
							        }
							        // Continue only if the File was successfully created
							        if (f != null) {
							        	currentMediaUri = Uri.fromFile(f);
							        	caputreIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri);
							            startActivityForResult(caputreIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
							        }
								   break;
							   }
						   }
					    });
						AlertDialog d = builder.create();
						d.show();
					} else {
						startActivityForResult(intent, 1);
					}
				} catch (Exception e) {
					Log.e(LOG_NAME, "Problem setting profile picture.");
				}
			}
		});
		
		return rootView;
	}

	private void setProfilePicture(File file, ImageView imageView) {
		if(file.exists() && file.canRead()) {
			try {
				imageView.setImageBitmap(MediaUtility.resizeAndRoundCorners(BitmapFactory.decodeStream(new FileInputStream(file)), 150));
			} catch(Exception e) {
				Log.e(LOG_NAME, "Problem setting profile picture.");
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		String filePath = null;
		switch (requestCode) {
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			MediaUtility.addImageToGallery(getActivity().getApplicationContext(), currentMediaUri);
			filePath = MediaUtility.getFileAbsolutePath(currentMediaUri, getActivity());
			
			break;
		case GALLERY_ACTIVITY_REQUEST_CODE:
			List<Uri> uris = getUris(data);
			for (Uri uri : uris) {
				filePath = MediaUtility.getPath(getActivity().getApplicationContext(), uri);
			}
			break;
		}
		if (filePath != null) {

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inSampleSize = 2;
			Bitmap bitmap = BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
			Bitmap b = MediaUtility.resizeAndRoundCorners(bitmap, 150);
			try {
				b = MediaUtility.orientBitmap(b, new File(filePath).getAbsolutePath(), false);
			} catch (Exception e) {
				Log.e(LOG_NAME, "failed to rotate image", e);
			}

			ImageView iv = (ImageView)getActivity().findViewById(R.id.profile_picture);
			iv.setImageBitmap(b);
			user.setLocalAvatarPath(filePath);
			UpdateProfileTask task = new UpdateProfileTask(user, getActivity());
			task.execute(filePath);
		}
	}

	private List<Uri> getUris(Intent intent) {
		List<Uri> uris = new ArrayList<Uri>();
		uris.addAll(getClipDataUris(intent));
		if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		return uris;
	}
	
	@TargetApi(16)
	private List<Uri> getClipDataUris(Intent intent) {
		List<Uri> uris = new ArrayList<Uri>();
		if (Build.VERSION.SDK_INT >= 16) {
			ClipData cd = intent.getClipData();
			if (cd != null) {
				for (int i = 0; i < cd.getItemCount(); i++) {
					uris.add(cd.getItemAt(i).getUri());
				}
			}
		}
		return uris;
	}
}