package com.mordor.wheredidipark;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends Activity {
	
	/** CONSTANTS */
	private static String TAG = "MainActivity";
	private static int LOCATION_RELEVANCE_DELAY = 5000; //milliseconds
	private static int LOCATION_FINDING_INTERVAL = 3; //seconds
	private static int MAX_LAT = 90; //degrees
	private static int MAX_LNG = 180; //degrees
	
	/** VARIABLES */
	private Location beaconLocation;
	private Location userLocation;
	private Button findBeaconButton;
	private Button setBeaconButton;
	private TextView timestamp;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private String locationProvider;
	
	@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_main);
    findBeaconButton = (Button) this.findViewById(R.id.find_beaconButton);
    setBeaconButton = (Button) this.findViewById(R.id.set_beaconButton);
    timestamp = (TextView) this.findViewById(R.id.timestampTextView);
    
    beaconLocation = new Location(LocationManager.GPS_PROVIDER);
    
    // Acquire a reference to the system Location Manager
    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    locationProvider = LocationManager.GPS_PROVIDER;

    // Define a listener that responds to location updates
    locationListener = new LocationListener() {
    	public void onLocationChanged(Location location) {
    		userLocation = location;
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {}
      public void onProviderEnabled(String provider) {
      	locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
      }
      public void onProviderDisabled(String provider) {}
    };
    
    locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
    
    if(storedLocationExists()) {
    	// Enable find beacon button
    	findBeaconButton.setEnabled(true);
    	findBeaconButton.setText("Find Your Car\n(Stored Location)");
    	
    	// Set timestamp textview to date location was set
    	timestamp.setText("Stored location found\n" + (new Date(beaconLocation.getTime())).toString());
    } else {
    	// Disable find beacon button
    	findBeaconButton.setEnabled(false);
    	findBeaconButton.setText("Find Your Car");
    	
    	// Set timestamp textview to error message
    	timestamp.setText("No Stored Location Found");
    }
	}
	
	@Override
	public void onPause() {
		// If beacon location exists, save it to Shared Preferences
		if(beaconLocation != null) {
			SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putFloat("latitude", (float)beaconLocation.getLatitude());
			editor.putFloat("longitude", (float)beaconLocation.getLongitude());
			editor.putLong("time", beaconLocation.getTime());
			editor.commit();
		}
		
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		userLocation = locationManager.getLastKnownLocation(locationProvider);
	}
	
	public void setBeacon(View v) throws InterruptedException {
		// Set beaconLocation to current location
		setBeaconLocation();
    
	}
	
	public void findBeacon(View v) {	
		userLocation = locationManager.getLastKnownLocation(locationProvider);
		
		// Check whether location was found
		if(userLocation == null) {
	   	// Notify user location not available
			Toast.makeText(this, "User location not available", Toast.LENGTH_SHORT).show();
		} else {
			//Open map activity with current location and beacon location
	    try {
	      Intent i = new Intent(this, BeaconMapActivity.class); 
	      
	      // Create locations extra
	      ArrayList<LatLng> locations = new ArrayList<LatLng>();
	      locations.add(new LatLng(beaconLocation.getLatitude(), beaconLocation.getLongitude()));
	      locations.add(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
	      
	      // Create times extra
	      ArrayList<Long> times = new ArrayList<Long>();
	      times.add(beaconLocation.getTime());
	      times.add(userLocation.getTime());
	      
	      // Put extras to Intent
	      i.putExtra("locations", locations);
	      i.putExtra("times", times);
	      
	      // Start map activity
	      this.startActivity(i);
	    } catch (Exception e) {
	      Log.e(TAG, "Exception error opening map");
	    }
		}
	}
	
	private void setBeaconLocation() throws InterruptedException {
  	// Disable buttons
   	findBeaconButton.setEnabled(false);
   	setBeaconButton.setEnabled(false);
   	
   	//TODO actually disable buttons
   	
   	boolean locationUpdated = false;
	  
   	// Set user location
   	userLocation = locationManager.getLastKnownLocation(locationProvider);
   	
	  // Check if returned location exists
	  if((userLocation != null) && (((new Date()).getTime() - userLocation.getTime()) < LOCATION_RELEVANCE_DELAY)) {
	    // Location successfully set
	    beaconLocation = userLocation;
	    locationUpdated = true;
	    	
	    // Indicate success
	    Toast.makeText(this, "Beacon location successfully set", Toast.LENGTH_SHORT).show();
	    	
	    // Set timestamp textview
	    timestamp.setText("New location set\n" + (new Date(beaconLocation.getTime())).toString());
	  } else {
	  	for(int i = 0; i < LOCATION_FINDING_INTERVAL; i++) {
	  		userLocation = locationManager.getLastKnownLocation(locationProvider);
	  		if((userLocation != null) && (((new Date()).getTime() - userLocation.getTime()) < LOCATION_RELEVANCE_DELAY)) {
	  			break;
	  		}
	  		Thread.sleep(1000);
	  	}
	  	
	  	if(userLocation != null) {
		    beaconLocation = userLocation;
		    locationUpdated = true;
	    } else {
	    	// Notify couldn't find location
	      Toast.makeText(this, "Timed out - couldn't retrieve current location", Toast.LENGTH_SHORT).show();
	    }
	  }
	  
	  if(locationUpdated) {
    	// Set find beacon button text
    	findBeaconButton.setText("Find Your Car\n(Newly-Set Location)");
    	timestamp.setText("New location set\n" + (new Date(beaconLocation.getTime())).toString());
	  } else {
	  	findBeaconButton.setText("Find Your Car\n(Stored Location)");
	  	timestamp.setText("Stored location found\n" + (new Date(beaconLocation.getTime())).toString());
	  }
	  
    // Enable buttons
    findBeaconButton.setEnabled(true);
    setBeaconButton.setEnabled(true);
	}
	
	private boolean storedLocationExists() {
	// Get stored location from SharedPreferences if it exists
    SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
    float latitude = sharedPref.getFloat("latitude", (MAX_LAT + 1));
    float longitude = sharedPref.getFloat("longitude", (MAX_LNG + 1));
    long time = sharedPref.getLong("time", 0);
    
    // Check whether saved location exists/is valid
    if((Math.abs(latitude) <= MAX_LAT) && (Math.abs(longitude) <= MAX_LNG) && (time > 0)) {
    	beaconLocation.setLatitude(latitude);
    	beaconLocation.setLongitude(longitude);
    	beaconLocation.setTime(time);   	
    	return true;
    }
    
    return false;
	}
}