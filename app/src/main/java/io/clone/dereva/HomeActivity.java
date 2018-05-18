package io.clone.dereva;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.clone.dereva.common.Common;
import io.clone.dereva.models.Token;
import io.clone.dereva.retrofit.IGoogleApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "HOME_ACTIVITY";
    private GoogleMap mMap;

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICES_RES_REQUEST = 7001;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;


    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference mDriversReference;
    GeoFire mGeoFire;
    Marker mCurrentMarker;

    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    //Car animation
    private List<LatLng> polyLinesList;
    private Marker carMarker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition, currentPosition;
    private int index, next;


//    private Button btnGo;
    private PlaceAutocompleteFragment place;
    private String destination;


    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;

    private IGoogleApi mService;

    //Presense system
    DatabaseReference onlineRef,currentUserRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        onlineRef=FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef=FirebaseDatabase.getInstance().getReference()
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //remove value from drivers table when driver disconnects
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        location_switch = (MaterialAnimatedSwitch) findViewById(R.id.location_switch);

        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline) {
                    FirebaseDatabase.getInstance().goOnline();
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are now online", Snackbar.LENGTH_SHORT).show();
                } else {
                    //disconnect
                    FirebaseDatabase.getInstance().goOffline();
                    stopLocationUpdates();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(), "You are now offline", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        polyLinesList=new ArrayList<>();
        place=(PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        place.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (location_switch.isChecked()){
                    destination=place.getAddress().toString();
                    destination=destination.replace(" ","+");
                    Log.d(TAG, "destination "+destination);
                    getDirection();
                }else{
                    Toast.makeText(HomeActivity.this, "Please set the location switch to online", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(HomeActivity.this, ""+status.toString(), Toast.LENGTH_SHORT).show();
            }
        });


        //drivers available
        mDriversReference = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_TABLE);
        mGeoFire = new GeoFire(mDriversReference);
        setUpLocation();

        mService= Common.getGoogleApi();
        updateFirebaseToken();
        Log.d(TAG, "onCreate: Created some token");
    }

    private void updateFirebaseToken() {
        FirebaseDatabase db=FirebaseDatabase.getInstance();
        DatabaseReference tokenRef=db.getReference(Common.TOKENS_TABLE);
        Token token=new Token(FirebaseInstanceId.getInstance().getToken());
        tokenRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        Log.d(TAG, "updateFirebaseToken: "+token.getToken());
    }

    //TODO Drivers Index On g
    private void getDirection() {
        currentPosition=new LatLng(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());
        String requestApi=null;
        try{
          requestApi="https://maps.googleapis.com/maps/api/directions/json?"+
                      "mode=driving&"+
                      "transit_routing_preference=less_driving&"+
                      "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                      "destination="+destination+"&"+
                      "key="+getResources().getString(R.string.google_maps_key);

            Log.d(TAG, "getDirection: "+requestApi);

            mService.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject=new JSONObject(response.body().toString());
                        JSONArray jsonArray=jsonObject.getJSONArray("routes");
                        for (int i=0; i<jsonArray.length(); i++){
                            JSONObject route=jsonArray.getJSONObject(i);
                            JSONObject poly=route.getJSONObject("overview_polyline");
                            String polyline=poly.getString("points");
                            polyLinesList=decodePoly(polyline);
                        }
                        //Adjust bounds
                        LatLngBounds.Builder builder=new LatLngBounds.Builder();
                        for (LatLng latLng:polyLinesList){
                            builder.include(latLng);
                        }
                        LatLngBounds bounds=builder.build();
                        CameraUpdate mCameraUpdate=CameraUpdateFactory.newLatLngBounds(bounds,2);
                        mMap.animateCamera(mCameraUpdate);
                        polylineOptions=new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.endCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polyLinesList);
                        greyPolyline=mMap.addPolyline(polylineOptions);

                        blackPolylineOptions=new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.endCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polyLinesList);
                        blackPolyline=mMap.addPolyline(blackPolylineOptions);


                        mMap.addMarker(new MarkerOptions().position(polyLinesList.get(polyLinesList.size()-1))
                        .title("Pickup Location"));
                        ValueAnimator polyLineAnimator=ValueAnimator.ofInt(0,100);
                        polyLineAnimator.setDuration(2000);
                        polyLineAnimator.setInterpolator(new LinearInterpolator());
                        polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                List<LatLng> points=greyPolyline.getPoints();
                                int percentValue=(int)animation.getAnimatedValue();
                                int size=points.size();
                                int newPoints=(int)(size*(percentValue/100.0f));
                                List<LatLng> p=points.subList(0,newPoints);
                                blackPolyline.setPoints(p);

                            }
                        });
                        polyLineAnimator.start();
                        carMarker=mMap.addMarker(new MarkerOptions().position(currentPosition)
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                        handler=new Handler();
                        index=-1;
                        next=1;
                        handler.postDelayed(drawPathRunnable,3000);



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });
        }
        catch (Exception e){
          e.printStackTrace();
        }
    }


    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }


    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(HomeActivity.this, permissions, MY_PERMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked()) {
                    displayLocation();
                }
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported by Google Play Services", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(HomeActivity.this, permissions, MY_PERMISSION_REQUEST_CODE);
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(HomeActivity.this, permissions, MY_PERMISSION_REQUEST_CODE);
            return;
        }

        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {
            if (location_switch.isChecked()) {
                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();
                String uuid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                mGeoFire.setLocation(uuid, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //update marker
                        if (mCurrentMarker != null)
                            mCurrentMarker.remove();
                        mCurrentMarker = mMap.addMarker(new MarkerOptions()
                                 //.icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                .position(new LatLng(latitude, longitude))
                                .title("You"));
                        //move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));
                        //draw rotate marker
                        rotateMarker(mCurrentMarker, -360, mMap);


                    }
                });
            }
        } else {
            Log.d(TAG, "displayLocation: CANNOT GET YOUR LOCATION");
        }

    }

    Runnable drawPathRunnable=new Runnable() {
        @Override
        public void run() {
            if (index<polyLinesList.size()-1){
               index++;
                next=index+1;
            }
            if (index<polyLinesList.size()-1){
                startPosition=polyLinesList.get(index);
                endPosition=polyLinesList.get(next);
            }
            final ValueAnimator valueAnimator=ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v=valueAnimator.getAnimatedFraction();
                    lng=v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat=v*endPosition.latitude+(1-v)*startPosition.latitude;
                    LatLng newPos=new LatLng(lat,lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f,0.5f);
                    carMarker.setRotation(getBearing(startPosition,newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                            .target(newPos)
                            .zoom(15.5f)
                            .build()
                    ));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this,3000);
        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat=Math.abs(startPosition.latitude-endPosition.latitude);
        double lng=Math.abs(startPosition.longitude-endPosition.longitude);
        if(startPosition.latitude<endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        }else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        }
        else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)((Math.toDegrees(Math.atan(lng/lat)))+180);
        }
        else if(startPosition.latitude<endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        }

        return -1;
    }

    private void rotateMarker(Marker currentMarker, final float i, GoogleMap map) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = currentMarker.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;
                mCurrentMarker.setRotation(-rot > 180 ? rot / 2 : rot);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(HomeActivity.this, permissions, MY_PERMISSION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();
    }

    /**
     * Check if location Perms has been granted by the user
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked()) {
                            displayLocation();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

}
