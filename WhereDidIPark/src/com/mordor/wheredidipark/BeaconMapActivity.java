package com.mordor.wheredidipark;

import java.util.ArrayList;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CheckBox;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class BeaconMapActivity extends Activity {
	
	/** CONSTANTS */
	private String TAG = "BeaconMapActivity";
  private final int LOCATION_UPDATE_INTERVAL = 5000; // Milliseconds
  
  /** VARIABLES */
  static BeaconMapActivity thisActivity;
  private String dirPoints;
  private CheckBox zoomCheck;
  private GoogleMap map;
  private LatLng beaconLatLng;
  private Date beaconTimeSet;
  private Handler timerHandler;
  private LocationManager locationManager;
  private String locationProvider;

  /* Returns current map activity instance for external control */
  public static BeaconMapActivity getInstance() {
    return thisActivity;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    thisActivity = this;
    setContentView(R.layout.activity_map);
    
    // Initialize timer handler
    this.timerHandler = new Handler();

    // Set check box
    zoomCheck = (CheckBox) this.findViewById(R.id.zoomCheckBox);

    // Get a handle to the Map Fragment
    map = ((MapFragment) getFragmentManager().findFragmentById(R.id.beacon_mapFragment)).getMap();

    // Enable your own location
    map.setMyLocationEnabled(true);
    
    // Initialize LocationManager
    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    locationProvider = LocationManager.GPS_PROVIDER;

    // Get passed in extras
    if (getIntent().getSerializableExtra("locations") != null) {
      @SuppressWarnings("unchecked")
      ArrayList<LatLng> locList = (ArrayList<LatLng>) getIntent().getSerializableExtra("locations");
      @SuppressWarnings("unchecked")
      ArrayList<Long> timeList = (ArrayList<Long>) getIntent().getSerializableExtra("times");

      // Set beacon LatLng, time
      beaconLatLng = locList.get(0);
      beaconTimeSet = new Date(timeList.get(0));

      // Set beacon location on map
      setBeaconLocationMarker(map);

      // Zoom in on the defined bounds
      zoomInOnBeacon(map, locList.get(1));
    }

    // Display home as up in Activity Bar
    this.getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  /* Zooms map view in to bounds defined by beacon and user */
  private void zoomInOnBeacon(final GoogleMap map, LatLng userLatLng) {
  	LatLngBounds.Builder builder = new LatLngBounds.Builder();
  	
  	// Include beacon location
    builder.include(beaconLatLng);
    
    // Include user location
    builder.include(userLatLng);
    
    final LatLngBounds bounds = builder.build();

    try {
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    } catch (Exception e) {
      // layout not yet initialized
      final View mapView = getFragmentManager().findFragmentById(R.id.beacon_mapFragment).getView();
      if (mapView.getViewTreeObserver().isAlive()) {
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(
        		new OnGlobalLayoutListener() {
              @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
              @Override
              public void onGlobalLayout() {
                mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
              }
            }
        );
      }
    }

  }

  /* Set location marker for beacon */
  private void setBeaconLocationMarker(GoogleMap map) {
    // Remove all existing markers, etc.
    map.clear();

    // Add location marker for beacon
    String tag = beaconTimeSet.toString();
    map.addMarker(new MarkerOptions().title("Car Location").snippet(tag).position(beaconLatLng));

    // Set beacon location as destination point for Directions
    this.dirPoints = ("http://maps.google.com/maps?f=&daddr=" + Double.toString(beaconLatLng.latitude) 
    		+ ", " + Double.toString(beaconLatLng.longitude));
  }

  /* Action taken on Get Directions button click */
  public void getDirections(View v) {
    if (this.dirPoints != "") {
      Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(this.dirPoints));
      startActivity(intent);
    }
  }

  /* Implements timer to get updated creep location data */
  Runnable zoomUpdater = new Runnable() {
    @Override
    public void run() {
      // Re-zooms map if box is checked
      if (zoomCheck.isChecked()) {
      	Location user = locationManager.getLastKnownLocation(locationProvider);
      	if(user != null) {
      		LatLng userLatLng = new LatLng(user.getLatitude(), user.getLongitude());
          zoomInOnBeacon(map, userLatLng);
      	}
      }

      timerHandler.postDelayed(zoomUpdater, LOCATION_UPDATE_INTERVAL);
    }
  }; 

  /* Starts location update timer */
  private void startTimer() {
    zoomUpdater.run();
  }

  /* Stops location update timer */
  private void stopTimer() {
    this.timerHandler.removeCallbacks(zoomUpdater);
  }

  /* Action taken when activity is resumed */
  @Override
  protected void onResume() {
    super.onResume();
    
    // Restart zoom update timer
    startTimer();
  }

  /* Action taken when activity is paused or destroyed */
  @Override
  protected void onPause() {
  	// Stop zoom update timer
    stopTimer(); 
    super.onPause();
  }

  /* Deals with Activity Bar and Menu item selections */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        if (NavUtils.getParentActivityName(this) != null) {
          NavUtils.navigateUpFromSameTask(this);
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}