package com.driving.self.sueno.sueno;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        TextToSpeech.OnInitListener {
      //  GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    int PROXIMITY_RADIUS = 1000;
    double latitude, longitude;
    double end_latitude, end_longitude;
    public List<LatLng> markers = new ArrayList<LatLng>();
    GroundOverlay overlayMap;
    GroundOverlay overlayTemp;
    boolean voiceToggle = false;

    boolean pauseFlag = false;
    double onStopLat, onStopLng;

    private static MapsActivity mapsActivity;
    private MapAnimator mapAnimator = new MapAnimator();

    public float oldBearing = 0;
    public float newBearing = 0;

    public String endLocation;
    LatLng latestLatLng;

    public float distanceTotal = 0;
    public float distanceTravelled = 0;
    public float distancePercentage = 0;

    private Integer THRESHOLD = 2;
    private DelayAutoCompleteTextView geo_autocomplete;
    private ImageView geo_autocomplete_clear;

    int currentPt;

    TextToSpeech tts;
    ImageView startRecognizer;
    ImageView emergencyButton;
    Spinner spinnerResult;
    TextView tbdestination;
    private static final int RQS_RECOGNITION = 1;

    public boolean isCurrentJourneyFlag = true;
    public float additionSeekBar = 0;

    DrivingClass drivingClass;
    TextView tv_driving_speed;
    TextView tv_max_speed;
    public int currentKmph = 0;
    public int maxSpeed = 50;
    public boolean isDriving = false;
    public int acceleration_delay = 200;
    public int stop_driving_delay = 200;

    boolean emergencyFlag;
    boolean googlePlacesFlag;

    View btmBar;
    View adminView;
    boolean adminViewFlag;
    boolean btmViewFlag;
    ImageButton btn_search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        drivingClass = new DrivingClass();
        btn_search = findViewById(R.id.btn_search_dest);

        tv_driving_speed = findViewById(R.id.tv_driving_speed);
        tv_max_speed = findViewById(R.id.tv_max_speed);

        startRecognizer = findViewById(R.id.btn_Voice);
        startRecognizer.setEnabled(false);
        spinnerResult = findViewById(R.id.va_spinner_result);
        startRecognizer.setOnClickListener(startRecognizerOnClickListener);

        tts = new TextToSpeech(this, this);

        btmBar = findViewById(R.id.btm_nav_bar);
        adminView = findViewById(R.id.admin_panel);

        setSeekBarProg(0, 0);

        geo_autocomplete_clear = findViewById(R.id.geo_autocomplete_clear);
        geo_autocomplete = findViewById(R.id.geo_autocomplete);
        geo_autocomplete.setThreshold(THRESHOLD);
        geo_autocomplete.setAdapter(new GeoAutoCompleteAdapter(this));

        geo_autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                GeoSearchResult result = (GeoSearchResult) adapterView.getItemAtPosition(position);
                geo_autocomplete.setText(result.getAddress());
            }
        });

        geo_autocomplete.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0)
                {
                    geo_autocomplete_clear.setVisibility(View.VISIBLE);
                }
                else
                {
                    geo_autocomplete_clear.setVisibility(View.GONE);
                }
            }
        });

        geo_autocomplete_clear.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // ToDo Auto-generated method stub
                geo_autocomplete.setText("");
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //Check if Google Play Services Available or not
        if (!CheckGooglePlayServices()) {
            //Log.d("onCreate", "Finishing test case since Google Play Services are not available");
            finish();
        }
        else {
            //Log.d("onCreate","Google Play Services available.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void slideUp(View view){
        //view.setVisibility(View.GONE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                view.getHeight() + 50);
        animate.setDuration(1000);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideBack(View view){
        //view.setVisibility(View.GONE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                view.getHeight() + 50,
                0);
        animate.setDuration(1000);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void adminSlideUp(View view){
        view.setVisibility(View.GONE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                view.getWidth() * -1,
                0,
                0);
        animate.setDuration(1000);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void adminSlideBack(View view){
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                view.getWidth() * -1,
                0,
                0,
                0);
        animate.setDuration(1000);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    private Button.OnClickListener startRecognizerOnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (voiceToggle == false){
                toggleNavbar("voice");
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help you?");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak("                 How can I help you?",TextToSpeech.QUEUE_FLUSH,null,null);
                } else {
                    tts.speak("                 How can I help you?", TextToSpeech.QUEUE_FLUSH, null);
                }

                startActivityForResult(intent, RQS_RECOGNITION);
            } else {
                toggleNavbar("voice_toggle");
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == RQS_RECOGNITION) & (resultCode == RESULT_OK)) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, result);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerResult.setAdapter(adapter);
            spinnerResult.setOnItemSelectedListener(spinnerResultOnItemSelectedListener);
        }
    }

    private Spinner.OnItemSelectedListener spinnerResultOnItemSelectedListener = new Spinner.OnItemSelectedListener() {
        String result;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedResult = parent.getItemAtPosition(position).toString();
            tbdestination = findViewById(R.id.geo_autocomplete);

            // Regex
            Collection<String> collection = new ArrayList<String>();
            String values = selectedResult; // input voice recognition result here

            for (String retval: values.split("(?!\\w)\\W")) {
                if (!retval.isEmpty())
                {
                    collection.add(retval);
                }
            }

            if (emergencyFlag == true){
                if (collection.contains("stop")){
                    drivingClass.quickStop();
                    stopAnimation();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("Emergency Stop Activated, Aborting Auto Drive Now",TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak("Emergency Stop Activated, Aborting Auto Drive Now", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    emergencyFlag = false;
                }
            } else if (emergencyFlag == false){
                if (collection.contains("restaurants")|(collection.contains("restaurant"))) {
                    getNearByRestaurants();
                } else {
                    tbdestination.setText(selectedResult);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(selectedResult,TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak(selectedResult, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    @Override
    public void onInit(int arg0) {
        startRecognizer.setEnabled(true);
        Log.i("", "STOP HERE");
    }

    public static MapsActivity getInstance(){
        if (mapsActivity == null) mapsActivity = new MapsActivity();
        return mapsActivity;
    }

    //Moving a custom marker along with the polyline
    public void movingMarker(LatLng newLatLng){

        longitude = newLatLng.longitude;
        latitude = newLatLng.latitude;

        LatLng oldLatLng = newLatLng;

        if (overlayMap != null){
            overlayTemp = overlayMap;
            oldLatLng = overlayTemp.getPosition();
        }
        oldBearing = newBearing;

        float results2[] = new float[10];
        Location.distanceBetween(latitude, longitude, end_latitude, end_longitude, results2);
        distanceTravelled = results2[0];

       // distancePercentage = (100 - ((distanceTravelled / distanceTotal) * 100)) + additionSeekBar;
        distancePercentage = (100 - ((distanceTravelled / distanceTotal) * 100));
        //User reaches destination
        if (Math.round(distancePercentage) >= 100){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toggleNavbar("drive_over");
                distancePercentage = 0;
                stopAnimation();
//                distanceTotal = 0;
//                distanceTravelled = 0;
                tts.speak("You have reached your destination",TextToSpeech.QUEUE_FLUSH,null,null);
                //markers.clear();
                mLastLocation.setLatitude(end_latitude);
                mLastLocation.setLongitude(end_longitude);
                geo_autocomplete.setText("");

            } else {
                geo_autocomplete.setText("");
                toggleNavbar("drive_over");
                distancePercentage = 0;
                distanceTotal = 0;
                distanceTravelled = 0;
                stopAnimation();
                tts.speak("You have reached your destination", TextToSpeech.QUEUE_FLUSH, null);
            }

            //Toast.makeText(MapsActivity.this, "You have reached your destination", Toast.LENGTH_LONG).show();
//            stopAnimation();
            drivingClass.quickStop();
            btn_search.setEnabled(true);
            geo_autocomplete.setEnabled(true);
        }

        setSeekBarProg(100, (int)Math.round(distancePercentage));

        //Calculate bearing to face next
        Double angle = Math.atan2((oldLatLng.longitude - newLatLng.longitude),(oldLatLng.latitude - newLatLng.latitude));
        angle = Math.toDegrees(angle);

        //If turn angle is beyond 10 deg, do the turning
        if ((Math.abs(oldBearing - angle) > 10 ) &&overlayMap != null){
            newBearing = Float.valueOf(String.valueOf(angle));
            //smoothTurning(overlayTemp, oldBearing, newBearing, oldLatLng);
        }

        final LatLng END_POINT = markers.get(markers.size()-1);

        smoothTurning(oldBearing, newBearing, oldLatLng, 100);

        overlayMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.car_model_white))
                .bearing(newBearing-180)
                .zIndex(10)
//                .position(newLatLng, 5f, 8.85f)); //LatLng of marker and Width x Height of marker
                .position(newLatLng, 80f, 141.6f));

        overlayTemp.remove();
    }


    public void smoothTurning(float prevBearing, float updateBearing, LatLng turnLatLng, int delay){

        CameraPosition cameraPosition =
        new CameraPosition.Builder()
                .target(turnLatLng)
                .tilt(90)
                .bearing(updateBearing-180)
                .zoom(17)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), delay, animationCancelableCallback);

    }

    private float bearingBetweenLatLngs(LatLng begin,LatLng end) {
        Location beginL= convertLatLngToLocation(begin);
        Location endL= convertLatLngToLocation(end);
        return beginL.bearingTo(endL);
    }

    private Location convertLatLngToLocation(LatLng latLng) {
        Location loc = new Location("someLoc");
        loc.setLatitude(latLng.latitude);
        loc.setLongitude(latLng.longitude);
        return loc;
    }

    //Set camera angle start
    CancelableCallback animationCancelableCallback = new CancelableCallback(){

        @Override
        public void onCancel() {}

        @Override
        public void onFinish() {

            if(++currentPt < markers.size()){
                float targetBearing = newBearing;
                LatLng targetLatLng = markers.get(currentPt);

                CameraPosition cameraPosition =
                        new CameraPosition.Builder()
                                .target(targetLatLng)
                                .tilt(90)
                                .bearing(targetBearing)
                                .zoom(17)
                                .build();

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 100, animationCancelableCallback);
                markers.get(currentPt);

            }
        }
    };

    CancelableCallback FinalCancelableCallback = new CancelableCallback() {

        @Override
        public void onFinish() {

        }

        @Override
        public void onCancel() {

        }
    };

    //Start the auto-driving animation
    private void startAnimation(){
        if(mMap != null) {
            mMap.animateCamera(
                    //CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom + 0.5f),
                    CameraUpdateFactory.zoomTo(17),
                    50,
                    animationCancelableCallback);

            currentPt = 0-1;

            mapAnimator.animateRoute(mMap, markers);

            //Start moving custom market here too
            movingMarker(latestLatLng);
        } else {
            //Toast.makeText(getApplicationContext(), "Map not ready", Toast.LENGTH_LONG).show();
        }
    }

    //Stop the auto-driving animation (When reach destination or stop abruptly)
    private void stopAnimation(){
        if(mMap != null) {
            markers.clear();
            onStopLat = mLastLocation.getLatitude();
            onStopLng = mLastLocation.getLongitude();

            mapAnimator.stopAnimator();
            isCurrentJourneyFlag = false;
        } else {
            //Toast.makeText(getApplicationContext(), "Map not ready", Toast.LENGTH_LONG).show();
        }
    }

    //Pause the auto-driving animation (When car momentarily pauses, Eg: Traffic light)
    //Slightly different usage scenarios as stopAnimation()
    private void pauseAnimation(){
        if(mMap != null) {
            pauseFlag = true;
            markers.clear();
            onStopLat = mLastLocation.getLatitude();
            onStopLng = mLastLocation.getLongitude();
            isCurrentJourneyFlag = true;
            additionSeekBar = distancePercentage;

            mapAnimator.stopAnimator();
        } else {
            //Toast.makeText(getApplicationContext(), "Map not ready", Toast.LENGTH_LONG).show();
        }
    }


    private boolean CheckGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                //mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            //mMap.setMyLocationEnabled(true);
        }

        MapStyleOptions styleOptions=MapStyleOptions.loadRawResourceStyle(this,R.raw.google_style);
        mMap.setMapStyle(styleOptions);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void resume(){

        pauseFlag = false;
        mMap.clear();
        List<Address> addressList = null;
        MarkerOptions markerOptions = new MarkerOptions();
        //Log.d("location = ", endLocation);

        if (!endLocation.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(endLocation, 5);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList != null) {
                for (int i = 0; i < addressList.size(); i++) {
                    Address myAddress = addressList.get(i);
                    LatLng latLng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                    markerOptions.position(latLng);
                    mMap.addMarker(markerOptions);
                    //mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), 50, animationCancelableCallback);

                    end_latitude = myAddress.getLatitude();
                    end_longitude = myAddress.getLongitude();
                }
            }

        }
        toDestination();
        if (googlePlacesFlag != false){
            autoDrive();
        }
    }

    public void onClick(View v)
    {
        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();

        switch(v.getId()) {
            case R.id.btn_search_dest: {

                if (isDriving == true){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("In midst of auto-driving",TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak("In midst of auto-driving", TextToSpeech.QUEUE_FLUSH, null);
                    }
                } else {
                    mMap.clear();
                    markers.clear();
                    distanceTotal = 0;
                    distanceTravelled = 0;
                    distancePercentage = 0;

                    SeekBar seekBar_top = new SeekBar(this);
                    seekBar_top = findViewById(R.id.sb_seekBar_top);

                    if (seekBar_top.getProgress() >0){
                        seekBar_top.setProgress(0);
                    }
                    isCurrentJourneyFlag = true;
                    EditText tf_location = findViewById(R.id.geo_autocomplete);
                    //String location = tf_location.getText().toString();
                    endLocation = tf_location.getText().toString();
                    List<Address> addressList = null;
                    MarkerOptions markerOptions = new MarkerOptions();

                    if (!endLocation.equals("")) {
                        Geocoder geocoder = new Geocoder(this);
                        try {
                            addressList = geocoder.getFromLocationName(endLocation, 5);
                            //Log.i("End location: ", endLocation);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (addressList != null) {
                            for (int i = 0; i < addressList.size(); i++) {
                                Address myAddress = addressList.get(i);
                                LatLng latLng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                                markerOptions.position(latLng);
                                mMap.addMarker(markerOptions);
                                //mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng),50, animationCancelableCallback);

                                end_latitude = myAddress.getLatitude();
                                end_longitude = myAddress.getLongitude();
                                }
                        }
                    }
                }
            }
            //Invoke toDestination & autoDrive if user clicks on on message dialog
            if (isDriving == true){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak("In midst of auto-driving",TextToSpeech.QUEUE_FLUSH,null,null);
                } else {
                    tts.speak("In midst of auto-driving", TextToSpeech.QUEUE_FLUSH, null);
                }
            } else {
                toDestination();
                if (googlePlacesFlag != false){
                    autoDrive();
                }
            }
            break;

            case R.id.btn_nearbyRestaurants:
                if (isDriving != true){
                    toggleNavbar("restaurant");
                    getNearByRestaurants();
                }
                break;

            case R.id.btn_Emergency:
                //Trigger voice assistant to listen for stop here
                if (drivingClass.getSpeed() > 0 && isDriving == true){
                    emergencyFlag = true;

                    btn_search.setEnabled(true);
                    geo_autocomplete.setEnabled(true);
                    geo_autocomplete.setText("");

                    stopAnimation();
                    markers.clear();
                    //mMap.clear();
                    toggleDriveMode("stopping");

                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
                    dlgAlert.setMessage("Emergency Stop Activated, Aborting Auto Drive Mode");
                    dlgAlert.setTitle("Emergency Stop");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                }
                            });

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("Emergency Stop Activated, Aborting Auto Drive Now",TextToSpeech.QUEUE_FLUSH,null,null);
                        drivingClass.quickStop();
                    } else {
                        tts.speak("Emergency Stop Activated, Aborting Auto Drive Now", TextToSpeech.QUEUE_FLUSH, null);
                        drivingClass.quickStop();
                    }
                    toggleNavbar("emergency");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("The car is stationary",TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak("The car is stationary", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }

                break;

//            case R.id.btn_Car:
//                if (btmViewFlag == false){
//                    slideUp(btmBar);
//                    btmViewFlag = true;
//                } else if (btmViewFlag == true){
//                    slideBack(btmBar);
//                    btmViewFlag = false;
//                }
//                break;
            case R.id.admin_emergency:
//                drivingClass.quickStop();
                if (pauseFlag == true){
                    resume();
                }
                break;
            case R.id.admin_gradual:
                if (drivingClass.getSpeed() > 0){
                    drivingClass.gradualStop();
                    btn_search.setEnabled(true);
                    geo_autocomplete.setEnabled(true);
                    stopAnimation();
                    markers.clear();
                    mMap.clear();
                    toggleDriveMode("stopping");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("Stopping Auto Drive Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak("Stopping Auto Drive Mode", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                break;
            case R.id.admin_pause:
                if (drivingClass.getSpeed() > 0){
                    drivingClass.quickStop();
                    btn_search.setEnabled(true);
                    geo_autocomplete.setEnabled(true);

                    pauseAnimation();
                    //markers.clear();
                    //mMap.clear();
                    toggleDriveMode("stopping");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak("Pausing auto drive mode",TextToSpeech.QUEUE_FLUSH,null,null);
                    } else {
                        tts.speak("Pausing auto drive mode", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                break;

            case R.id.admin_max50:
                drivingClass.setMaxSpeed(50);
                break;

            case R.id.admin_max90:
                drivingClass.setMaxSpeed(90);
                break;

            case R.id.btn_admin_panel:
                if (adminViewFlag == false){
                    adminSlideBack(adminView);
                    adminViewFlag = true;
                } else if (adminViewFlag == true){
                    adminSlideUp(adminView);
                    adminViewFlag = false;
                }
                break;

            case R.id.btn_hl_Voice:
                toggleNavbar("voice_toggle");
                break;

            case R.id.btm_nav_bar:
                //goyang
                if (btmViewFlag == false){
                    slideUp(btmBar);
                    btmViewFlag = true;
                } else if (btmViewFlag == true){
                    slideBack(btmBar);
                    btmViewFlag = false;
                }
        }
    }

    public void toggleNavbar(String buttonPressed){
        //Left Navbar
        ImageView car = findViewById(R.id.btn_Car);
        ImageView hl_car = findViewById(R.id.btn_hl_Car);

        ImageView emergency = findViewById(R.id.btn_Emergency);
        ImageView hl_emergency = findViewById(R.id.btn_hl_Emergency);

        ImageView voice = findViewById(R.id.btn_Voice);
        ImageView hl_voice = findViewById(R.id.btn_hl_Voice);

        ImageView restaurants = findViewById(R.id.btn_nearbyRestaurants);
        ImageView hl_restaurants = findViewById(R.id.btn_hl_nearbyRestaurants);

        //Right Navbar
        ImageView music = findViewById(R.id.btn_Music);
        ImageView hl_music = findViewById(R.id.btn_hl_Music);

        ImageView video = findViewById(R.id.btn_Video);
        ImageView hl_video = findViewById(R.id.btn_hl_Video);

        ImageView settings = findViewById(R.id.btn_Settings);
        ImageView hl_settings = findViewById(R.id.btn_hl_Settings);

        switch (buttonPressed){
            case "car":
                car.setVisibility(View.GONE);
                hl_car.setVisibility(View.VISIBLE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "emergency":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.GONE);
                hl_emergency.setVisibility(View.VISIBLE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "voice":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.GONE);
                hl_voice.setVisibility(View.VISIBLE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "voice_toggle":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "restaurant":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.GONE);
                hl_restaurants.setVisibility(View.VISIBLE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "music":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.GONE);
                hl_music.setVisibility(View.VISIBLE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "video":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.GONE);
                hl_video.setVisibility(View.VISIBLE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "settings":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.GONE);
                hl_settings.setVisibility(View.VISIBLE);
                break;

            case "controller":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;

            case "drive_over":
                car.setVisibility(View.VISIBLE);
                hl_car.setVisibility(View.GONE);

                emergency.setVisibility(View.VISIBLE);
                hl_emergency.setVisibility(View.GONE);

                voice.setVisibility(View.VISIBLE);
                hl_voice.setVisibility(View.GONE);

                restaurants.setVisibility(View.VISIBLE);
                hl_restaurants.setVisibility(View.GONE);

                music.setVisibility(View.VISIBLE);
                hl_music.setVisibility(View.GONE);

                video.setVisibility(View.VISIBLE);
                hl_video.setVisibility(View.GONE);

                settings.setVisibility(View.VISIBLE);
                hl_settings.setVisibility(View.GONE);
                break;
        }

    }

    public void toggleDriveMode(String mode){

        ImageView drive = findViewById(R.id.drive);
        ImageView stationary = findViewById(R.id.stationary);
        ImageView neutral = findViewById(R.id.neutral);
        ImageView parking = findViewById(R.id.parking);
        ImageView reverse = findViewById(R.id.reverse);
        ImageView stopping = findViewById(R.id.stopping);

        switch (mode){
            case "stationary":
                stationary.setVisibility(View.VISIBLE);
                drive.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
            case "drive":
                drive.setVisibility(View.VISIBLE);
                stationary.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
            case "reverse":
                reverse.setVisibility(View.VISIBLE);
                stationary.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                drive.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
            case "neutral":
                neutral.setVisibility(View.VISIBLE);
                stationary.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                drive.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
            case "parking":
                parking.setVisibility(View.VISIBLE);
                stationary.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                drive.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
            case "stopping":
                stopping.setVisibility(View.VISIBLE);
                stationary.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                drive.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                break;
            default:
                stationary.setVisibility(View.VISIBLE);
                drive.setVisibility(View.GONE);
                neutral.setVisibility(View.GONE);
                parking.setVisibility(View.GONE);
                reverse.setVisibility(View.GONE);
                stopping.setVisibility(View.GONE);
                break;
        }
    }

    public void toDestination(){
        googlePlacesFlag = false;
        Object dataTransfer[] = new Object[2];
        mMap.clear();
        dataTransfer = new Object[3];
        String url = getDirectionsUrl();
        GetDirectionsData getDirectionsData = new GetDirectionsData();
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;
        dataTransfer[2] = new LatLng(end_latitude, end_longitude);

        if (end_latitude <= 1.0 && end_longitude <= 1.0){
            googlePlacesFlag = false;
        } else {
            googlePlacesFlag = true;
        }

        getDirectionsData.execute(dataTransfer);

        if (googlePlacesFlag == true){
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(end_latitude, end_longitude));
            markerOptions.title("Destination");

            float results[] = new float[10];
            Location.distanceBetween(latitude, longitude, end_latitude, end_longitude, results);
            markerOptions.snippet("Distance = " + results[0] + " metres");
            mMap.addMarker(markerOptions);

            //distanceTotal = Integer.valueOf(String.valueOf(results[0]));
            distanceTotal = results[0];
        } else if (googlePlacesFlag == false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LatLng latLng = new LatLng(latitude, longitude);
                tts.speak("You have entered a wrong address",TextToSpeech.QUEUE_FLUSH,null,null);
                overlayMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromResource(R.drawable.car_model_white))
                        .bearing(newBearing)
                        .zIndex(1)
                        .position(latLng, 80f, 141.6f));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                tts.speak("You have entered a wrong address", TextToSpeech.QUEUE_FLUSH, null);
                overlayMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromResource(R.drawable.car_model_white))
                        .bearing(newBearing)
                        .zIndex(1)
                        .position(latLng, 80f, 141.6f));
            }
        }
    }

    public void autoDrive(){
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {

                    try {
                        final LatLng START_POINT = markers.get(0);
                        final LatLng END_POINT = markers.get(markers.size()-1);

                        latestLatLng = START_POINT;

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(START_POINT); //Later store values of latlong of start & end pts from arraylist
                        builder.include(END_POINT);
                        LatLngBounds bounds = builder.build();

                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        googlePlacesFlag = false;
                    }

                    if (googlePlacesFlag == true){
                        startAnimation();
                        drivingClass.startDriving();
                        isDriving = true;
                        toggleDriveMode("drive");
                        toggleNavbar("car");
                        btn_search.setEnabled(false);
                        geo_autocomplete.setEnabled(false);
                        //Toast.makeText(MapsActivity.this, "Initialize Auto Driving", Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            tts.speak("Initializing auto driving mode",TextToSpeech.QUEUE_FLUSH,null,null);
                        } else {
                            tts.speak("Initializing auto driving mode", TextToSpeech.QUEUE_FLUSH, null);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            tts.speak("You have entered a wrong address",TextToSpeech.QUEUE_FLUSH,null,null);
                            geo_autocomplete.setText("");
                        } else {
                            tts.speak("You have entered a wrong address", TextToSpeech.QUEUE_FLUSH, null);
                            geo_autocomplete.setText("");
                        }
                    }
                }
            }
        };

        handler.sendEmptyMessageDelayed(1, 1000);
    }

    private void getNearByRestaurants(){

        if (isDriving != true){
            Object dataTransfer[] = new Object[2];
            GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();

            mMap.clear();
            dataTransfer = new Object[2];
            String restaurant = "restaurant";
            String url = getUrl(latitude, longitude, restaurant);
            getNearbyPlacesData = new GetNearbyPlacesData();
            dataTransfer[0] = mMap;
            dataTransfer[1] = url;

            getNearbyPlacesData.execute(dataTransfer);
            //Toast.makeText(MapsActivity.this, "Showing Nearby Restaurants", Toast.LENGTH_LONG).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak("Showing nearby restaurants",TextToSpeech.QUEUE_FLUSH,null,null);
            } else {
                tts.speak("Showing nearby restaurants", TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private String getDirectionsUrl()
    {
        StringBuilder googleDirectionsUrl = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        googleDirectionsUrl.append("origin="+latitude+","+longitude);
        googleDirectionsUrl.append("&destination="+end_latitude+","+end_longitude);
        googleDirectionsUrl.append("&key="+"AIzaSyCAcfy-02UHSu2F6WeQ1rhQhkCr51eBL9g");

        return googleDirectionsUrl.toString();
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace)
    {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyBj-cnmMUY21M0vnIKz0k3tD3bRdyZea-Y");

        return (googlePlacesUrl.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }


        Log.i("", "onlocationchanged !!");

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        LatLng latLng = new LatLng(latitude, longitude);

        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(latLng)
                        .tilt(90)
                        .bearing(newBearing)
                        .zoom(17)
                        .build();

        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(cameraPosition),50, animationCancelableCallback);

        overlayMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.car_model_white))
                .bearing(newBearing)
                .zIndex(1)
                .position(latLng, 80f, 141.6f));

        //Toast.makeText(MapsActivity.this,"Your Current Location", Toast.LENGTH_LONG).show();

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the contacts-related task if needed
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        //Disable googlemaps location enable bluedot
                        //mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    //Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public class GetDirectionsData extends AsyncTask<Object,String,String> {

        String url;
        String googleDirectionsData;
        String duration, distance;
        LatLng latLng;

        @Override
        protected String doInBackground(Object... objects) {
            mMap = (GoogleMap)objects[0];
            url = (String)objects[1];
            latLng = (LatLng)objects[2];

            DownloadUrl downloadUrl = new DownloadUrl();
            try {
                googleDirectionsData = downloadUrl.readUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return googleDirectionsData;
        }

        @Override
        protected void onPostExecute(String s) {
            final String[] directionsList;
            DataParser parser = new DataParser();
            directionsList = parser.parseDirections(s);

            if (directionsList != null){

                displayDirection(directionsList);

            } else {
                googlePlacesFlag = false;
            }
        }

        //On post execute method
        public List<LatLng> displayDirection(String[] directionsList)
        {
            int count = directionsList.length;
            for(int i = 0;i<count;i++)
            {
                PolylineOptions options = new PolylineOptions();
                options.color(Color.YELLOW);
                options.width(50f);
                options.addAll(PolyUtil.decode(directionsList[i]));

                //1st regex filter: Clean all alphabets and special characters except (.) and (,)
                String str = options.getPoints().toString().replaceAll("[^\\d.,]", "");

                //2nd filter: Put 1st regex into a list and only take 1st two values for latlng
                ArrayList latlngList = new ArrayList(Arrays.asList(str.split(",")));

                //Loop through each iteration to get the entire curve of the polyline's coordinates
                for (int j=0; j<latlngList.size(); j+=2){
                    double listLat = Double.parseDouble(latlngList.get(j).toString());
                    double listLong = Double.parseDouble(latlngList.get(j+1).toString());
                    LatLng listLatLong = new LatLng(listLat, listLong);
                    markers.add(listLatLong);
//                            mMap.addPolyline(options);
                }
                mMap.addPolyline(options);
            }
            return markers;
        }
    }


    public class MapAnimator {
        private AnimatorSet RunAnimatorSet;

        //private Polyline backgroundPolyline;
        private Polyline foregroundPolyline;
        private PolylineOptions optionsForeground;
        int counter = 0;

        private MapAnimator(){

        }

        public void stopAnimator(){
            RunAnimatorSet.removeAllListeners();
            RunAnimatorSet.cancel();
        }

        public void pauseAnimator(){
            RunAnimatorSet.removeAllListeners();
            RunAnimatorSet.cancel();
        }

        public void animateRoute(GoogleMap googleMap, List<LatLng> drivingRoute) {
            if (RunAnimatorSet == null){
                RunAnimatorSet = new AnimatorSet();
            } else {
                RunAnimatorSet.removeAllListeners();
                RunAnimatorSet.end();
                RunAnimatorSet.cancel();

                RunAnimatorSet = new AnimatorSet();
            }

            //Reset the polylines
            if (foregroundPolyline != null) foregroundPolyline.remove();
          //  if (backgroundPolyline != null) backgroundPolyline.remove();

            //PolylineOptions optionsBackground = new PolylineOptions().add(drivingRoute.get(0)).color(Color.RED).width(15);
            //backgroundPolyline = googleMap.addPolyline(optionsBackground);

            optionsForeground = new PolylineOptions().add(drivingRoute.get(0)).color(Color.YELLOW).width(50f);
            foregroundPolyline = googleMap.addPolyline(optionsForeground);

            final ValueAnimator percentageCompletion = ValueAnimator.ofInt(0, 100);
            percentageCompletion.setDuration(2000);
            percentageCompletion.setInterpolator(new DecelerateInterpolator());
            percentageCompletion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
//                    if (backgroundPolyline != null){
//                        List<LatLng> foregroundPoints = backgroundPolyline.getPoints();
//                        int percentageValue = (int) animation.getAnimatedValue();
//                        int pointcount = foregroundPoints.size();
//                        int countTobeRemoved = (int) (pointcount * (percentageValue / 100.0f));
//                        List<LatLng> subListTobeRemoved = foregroundPoints.subList(0, countTobeRemoved);
//                        subListTobeRemoved.clear();
//
//                        foregroundPolyline.setPoints(foregroundPoints);
//                    }
                }
            });
            percentageCompletion.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    if (backgroundPolyline != null){
//                        foregroundPolyline.setColor(Color.YELLOW);
//                        foregroundPolyline.setPoints(backgroundPolyline.getPoints());
//                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });


            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), Color.YELLOW, Color.YELLOW);
            colorAnimation.setInterpolator(new AccelerateInterpolator());
            colorAnimation.setDuration(1200); // milliseconds

            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    foregroundPolyline.setColor((int) animator.getAnimatedValue());
                }

            });

            ObjectAnimator foregroundRouteAnimator = ObjectAnimator.ofObject(this, "routeIncreaseForward", new RouteEvaluator(), drivingRoute.toArray());
            foregroundRouteAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            foregroundRouteAnimator.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //backgroundPolyline.setPoints(foregroundPolyline.getPoints());
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            //Value to adjust the speed of the polyline
            foregroundRouteAnimator.setDuration(50000); //Animation speed
//        foregroundRouteAnimator.start();

            RunAnimatorSet.playSequentially(foregroundRouteAnimator,
                    percentageCompletion);
            RunAnimatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //secondLoopRunAnimSet.start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            RunAnimatorSet.start();
        }

        /**
         * This will be invoked by the ObjectAnimator multiple times. Mostly every 16ms.
         **/
        public void setRouteIncreaseForward(LatLng endLatLng) {
            List<LatLng> foregroundPoints = foregroundPolyline.getPoints();
            foregroundPoints.add(endLatLng);
            foregroundPolyline.setPoints(foregroundPoints);
            movingMarker(endLatLng);
        }
    }

    // Animator Class' AsyncTask - Deprecated
    public class animatorAsync extends AsyncTask<LatLng, Integer, Integer>{

        LatLng asyncLatLng;

        @Override
        protected void onPreExecute(){
            //tvCurrentSpd.setText("onPre-> Current Speed: " + currentKmph);
        }

        @Override
        protected Integer doInBackground(LatLng... params){
            try {
                //MapsActivity.getInstance().movingMarker(asyncLatLng);
                movingMarker(asyncLatLng);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... update){
            //tvCurrentSpd.setText("onProgUpdate -> Current Speed: " + currentKmph);
        }
    }

    public void setSeekBarProg(int maxBar, int ProgressVal){ //Variables: setSeekBarProg(int Max, int ProgressVal)
        SeekBar seekBar_top = new SeekBar(this);
        seekBar_top = findViewById(R.id.sb_seekBar_top);

        if (seekBar_top.getProgress() < ProgressVal){
            seekBar_top.setProgress(ProgressVal);
        }

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100);
        seekBar_top.setLayoutParams(lp);

        seekBar_top.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent){
                return true;
            }
        });
    }


    //DrivingClass
    public class DrivingClass extends AppCompatActivity {

        public DrivingClass drivingClass(){
            if (drivingClass == null){
                drivingClass = new DrivingClass();
            }
            return drivingClass;
        }

        public void throttleSpeed(){
            if (currentKmph >= (maxSpeed - 3)){
                currentKmph--;
                currentKmph--;
            }
        }

        public int getSpeed(){
            return currentKmph;
        }

        public void resetSpeed(){
            currentKmph = 0;
        }

        public void setMaxSpeed(int max){
            maxSpeed = max;
            tv_max_speed.setText("" + maxSpeed);
        }

        public void decelerate(){
            for (int a = 0; a < 5; a++){
                currentKmph++;
            }
        }

        public void accelerate(){
            for (int a = 0; a < 5; a++){
                currentKmph--;
            }
        }

        public void isSpeedMax(){
            if (currentKmph >= (maxSpeed - 1)){
                currentKmph--;
            }
        }

        public void gradualStop(){
            isDriving = false;
            if (currentKmph > 0){
                toggleDriveMode("stopping");
                stop_driving_delay = 200;
                new DrivingClass.stopAsync().execute(stop_driving_delay);
            }
        }

        public void quickStop(){
            isDriving = false;
            if (currentKmph > 0){
                toggleDriveMode("stopping");
                stop_driving_delay = 50;
                new DrivingClass.stopAsync().execute(stop_driving_delay);
            }
        }

        public void startDriving(){
            isDriving = true;
            if (currentKmph < maxSpeed){
                toggleDriveMode("drive");
                new DrivingClass.driveAsync().execute(acceleration_delay);
            }
        }

        //AsyncTask for startDriving() method
        public class driveAsync extends AsyncTask<Integer, Integer, Integer>{

            @Override
            public void onPreExecute(){
                tv_driving_speed.setText("" + currentKmph);
            }

            @Override
            public Integer doInBackground(Integer... params){
                while (isDriving == true && currentKmph < maxSpeed) {
                    try {
                        currentKmph++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_driving_speed.setText("" + currentKmph);
                                throttleSpeed();
                                isSpeedMax();
                            }
                        });
                        Thread.currentThread();
                        Thread.sleep(acceleration_delay);

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            public void onProgressUpdate(Integer... update){
                tv_driving_speed.setText("" + currentKmph);
            }
        }

        //AsyncTask for gradualStop() or stopDriving() methods
        public class stopAsync extends AsyncTask<Integer, Integer, Integer>{

            @Override
            public void onPreExecute(){
                tv_driving_speed.setText("" + currentKmph);
            }

            @Override
            public Integer doInBackground(Integer... params){
                while (currentKmph > 0) {

                    try {
                        currentKmph--;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_driving_speed.setText("" + currentKmph);
                                if (currentKmph == 0){
                                    toggleDriveMode("stationary");
                                }
                            }
                        });
                        Thread.currentThread();
                        Thread.sleep(stop_driving_delay);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            public void onProgressUpdate(Integer... update){
                tv_driving_speed.setText("" + currentKmph);
            }
        }
    }

}


