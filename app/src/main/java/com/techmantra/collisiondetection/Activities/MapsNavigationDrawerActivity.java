package com.techmantra.collisiondetection.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techmantra.collisiondetection.Models.User;
import com.techmantra.collisiondetection.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapsNavigationDrawerActivity extends AppCompatActivity
        implements OnMapReadyCallback ,NavigationView.OnNavigationItemSelectedListener,SensorEventListener {

    private static final String TAG = "NavigatnDrwrActvty";
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseDatabase mDB;
    private DatabaseReference mDBref;
    private DatabaseReference mDbref2;

    private FirebaseUser mUser;
    private Location mlocation;

    private Location lastLocation;

    private List<User> allUsersList;
    private List<User> userLocationList;

    private SensorManager sensorManager;
    Sensor accelerometer;

    private float currSpeed = 0;
    private User currUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        lastLocation = null;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MapsNavigationDrawerActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        currUser = new User();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //navigation drawer that is available when we slide the screen from left side
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    //here we are checking if the permissions for location are granted or not
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, locationListener);
            }
        }
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

        //location manager and location listener is used to get location updates at regular intervals of tine
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {

            //this function is called every time when our device's location is changed
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                //we are taking last location to set the bearing or direction of the car
                if (lastLocation == null) {
                    lastLocation = mlocation;
                }
                float bearing = lastLocation.bearingTo(mlocation);

                //map is cleared that means all the previous icons are removed every time our location is changed
                mMap.clear();

                //getting latitude and longitude for our current location and adding a marker on the same
                LatLng locLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(locLatLng).title("your Speed : " + String.valueOf(Float.parseFloat(String.valueOf(currSpeed)))).
                        icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellowcar128bit))
                        .anchor(0.5f, 0.5f).rotation(bearing).flat(true)).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locLatLng, 16));
                //Log used for debugging
                Log.d("Location is :", location.toString());

//                speed from LocationManager
//                double currSpeed = location.getSpeed();

                //speed calculated from accelerometer
                Log.d("speed -", String.valueOf(currSpeed));

                //Taking an instance of firebase database that we use to store the data of every user
                mDB = FirebaseDatabase.getInstance();

                Log.d("currUser", "onLocationChanged: " + mUser.getUid());
                mDBref = mDB.getReference().child("LocationInfo");
                mDBref.keepSynced(true);

                //uploading our data to firebase database
                if (mUser != null) {

                    User currentUser = new User(mUser.getUid(), currSpeed, location.getLatitude(), location.getLongitude());
                    mDBref.child(mUser.getUid()).setValue(currentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MapsNavigationDrawerActivity.this, "Location sent to server!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    User currentUser = new User(currSpeed, location);
                    mDBref.setValue(currentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MapsNavigationDrawerActivity.this, "Location sent to server!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                //updating last location every time we are moving out of this function
                lastLocation = location;

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }


        };

        //Requesting for the permissions from user
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            //    here to request the missing permissions, and then overriding
            //    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            Log.d("called_1", "onMapReady: ");
        } else {
            Log.d("called_2", "onMapReady: ");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 2 , locationListener);
            mMap.setMyLocationEnabled(true);
            //  locationListener.onLocationChanged(null);
        }

        //calling retrieveAllUsers function so that we can access all the available users of this app from firebase
        retrieveAllUsers();

    }

    //This function is used to get information of all the users location and speed from firebase
    private void retrieveAllUsers() {
        allUsersList = new ArrayList<>();

        mDBref = FirebaseDatabase.getInstance().getReference("UserDetails");
        mDBref.addValueEventListener(new ValueEventListener() {

            //this function is called every time a new user is added
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    User appUser = snap.getValue(User.class);
                    allUsersList.add(appUser);
                    Log.d("userName_fetched-", appUser.getUserName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        userLocationList = new ArrayList<>();
        mDbref2 = FirebaseDatabase.getInstance().getReference("LocationInfo");
        mDbref2.addValueEventListener(new ValueEventListener() {
           //this function is called when the location is changed for any user
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    User appUser = snap.getValue(User.class);
                    Log.d("userLoc--", String.valueOf(appUser.getId()));
                    userLocationList.add(appUser);
                    if (appUser.getId() != mUser.getUid()) {
                        LatLng appUserLatLng = new LatLng(appUser.latitude, appUser.longitude);
                        mMap.addMarker(new MarkerOptions().position(appUserLatLng).title("Speed : " + appUser.getSpeed()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.sports_car_red)))
                                .showInfoWindow();
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //On back button pressed this is for navigation drawer
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //This function is used to inflate the Navigation drawer
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation_drawer, menu);

        return true;
    }

//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.about_us_item) {
            // Handle the camera action
            Toast.makeText(MapsNavigationDrawerActivity.this,"About us item clicked from menu",Toast.LENGTH_LONG).show();
        }
        else if (id == R.id.signOutMenuItem) {
            mAuth.signOut();
            locationManager.removeUpdates(locationListener);
            startActivity(new Intent(MapsNavigationDrawerActivity.this,LoginActivity.class));
            finish();
        }


            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        float xOld = 0;
        float yOld = 0;
        float zOld = 0;
        long oldTime = 0;

        //this function is for getting the data from the phone's accelerometer sensor
        @Override
        public void onSensorChanged (SensorEvent event){
//        Log.d(TAG+" x- :",event.values[0]+" y- :"+event.values[1]+" z- :" + event.values[2]);
            //getting the x , y and z coordinates for our location from acceleromter sensor
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long currentTime = System.currentTimeMillis();
            if ((currentTime - oldTime) > 1000) {
                long timeDifference = currentTime - oldTime;
                oldTime = currentTime;
                //calculation of speed from standard formula of speed
                currSpeed = Math.abs((x + y + z - xOld - yOld - zOld) / timeDifference * 1000);
//            currUser.setSpeed(currSpeed);
                Log.d(TAG, "speed by acceleromter--" + String.valueOf(currSpeed));
                currUser.setSpeed(currSpeed);
            }
            //updating our old x,y and z coordinates every time
            xOld = x;
            yOld = y;
            zOld = z;
        }

        @Override
        public void onAccuracyChanged (Sensor sensor,int accuracy){

        }

    }

