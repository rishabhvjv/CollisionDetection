package com.techmantra.collisiondetection.Activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback  {

    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseDatabase mDB;
    private DatabaseReference mDBref;
    private DatabaseReference mDbref2;

    private FirebaseUser mUser;
    private Location mlocation;

    private Location lastLocation ;

    private List<User> allUsersList;
    private List<User> userLocationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        lastLocation = null ;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
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

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                if(lastLocation == null){
                    lastLocation = mlocation;
                }
                float bearing = lastLocation.bearingTo(mlocation);
                calculateSpeedManual(lastLocation,mlocation);
                mMap.clear();
                LatLng locLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(locLatLng).title("you are here"+ location.getSpeed()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellowcar128bit))
                        .anchor(0.5f,0.5f).rotation(bearing).flat(true)).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locLatLng , 16));

                Log.d("Location is :", location.toString());

                float currSpeed = location.getSpeed();
                Log.d("speed -", String.valueOf(currSpeed));

                mDB = FirebaseDatabase.getInstance();

                Log.d("currUser", "onLocationChanged: " + mUser.getUid());
                mDBref = mDB.getReference().child("LocationInfo");
                mDBref.keepSynced(true);

                if(mUser!= null) {

                    User currentUser = new User(mUser.getUid(),currSpeed,location.getLatitude(),location.getLongitude());
                    mDBref.child(mUser.getUid()).setValue(currentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MapsActivity.this, "Location sent to server!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    User currentUser = new User(currSpeed,location);
                    mDBref.setValue(currentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MapsActivity.this, "Location sent to server!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                lastLocation = location;

            }

            private void calculateSpeedManual(Location lastLocation, Location mlocation) {

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
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, locationListener);
            mMap.setMyLocationEnabled(true);
          //  locationListener.onLocationChanged(null);
        }
        
        retrieveAllUsers();

    }

    private void retrieveAllUsers() {
        allUsersList = new ArrayList<>();

        mDBref = FirebaseDatabase.getInstance().getReference("UserDetails");
        mDBref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()){
                    User appUser = snap.getValue(User.class);
                    allUsersList.add(appUser);
                    Log.d("userName_fetched-",appUser.getUserName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        userLocationList = new ArrayList<>();
        mDbref2 = FirebaseDatabase.getInstance().getReference("LocationInfo");
        mDbref2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()){
                    User appUser = snap.getValue(User.class);
                    Log.d("userLoc--", String.valueOf(appUser.getId()));
                    userLocationList.add(appUser);
                    if(appUser.getId() != mUser.getUid()) {
                        LatLng appUserLatLng = new LatLng(appUser.latitude, appUser.longitude);
                        mMap.addMarker(new MarkerOptions().position(appUserLatLng).title("user" + appUser.getSpeed()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.sports_car_red)))
                                .showInfoWindow();
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main_menu,menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if(item.getItemId() == R.id.action_signOut){
//            mAuth.signOut();
//            locationManager.removeUpdates(locationListener);
//            startActivity(new Intent(MapsActivity.this ,LoginActivity.class));
//
//        }
//        return super.onOptionsItemSelected(item);
//    }

}
