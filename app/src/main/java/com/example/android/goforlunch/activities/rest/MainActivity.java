package com.example.android.goforlunch.activities.rest;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.activities.auth.AuthSignInActivity;
import com.example.android.goforlunch.atl.ATLInitApiTextSearchRequests;
import com.example.android.goforlunch.data.AppDatabase;
import com.example.android.goforlunch.data.RestaurantEntry;
import com.example.android.goforlunch.data.viewmodel.MainViewModel;
import com.example.android.goforlunch.helpermethods.ToastHelper;
import com.example.android.goforlunch.models.modelnearby.LatLngForRetrofit;
import com.example.android.goforlunch.models_delete.PlaceInfo;
import com.example.android.goforlunch.pageFragments.FragmentCoworkersView;
import com.example.android.goforlunch.pageFragments.FragmentRestaurantListViewTRIAL;
import com.example.android.goforlunch.pageFragments.FragmentRestaurantMapViewTRIAL;
import com.example.android.goforlunch.placeautocompleteadapter.PlaceAutocompleteAdapter;
import com.example.android.goforlunch.pojo.User;
import com.example.android.goforlunch.strings.StringValues;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// TODO: 18/05/2018 Check if there is internet connection
// TODO: 21/05/2018 Add a flag so when we come back from RestaurantActivity when don't do API Requests again
// TODO: 21/05/2018 Add a button to do the API Requests again
// TODO: 24/05/2018 Add JOIN GROUP in NavigationDrawer
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Loaders id
    private static final int ID_LOADER_INIT_GENERAL_API_REQUESTS = 1;

    private FirebaseAuth auth;

    //Values to store user's name
    private String name = "anonymous";
    private String email = "anon@anonymous.com";
    private List<User> listOfUsers;

    //Widgets
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private BottomNavigationView navigationView;

    //------------------------------------------------

    //ERROR that we are going to handle if the user doesn't have the correct version of the
    //Google Play Services
    private static final int ERROR_DIALOG_REQUEST = 9001;

    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 17f;
    private static final float LATITUDE_BOUND = 0.007f;
    private static final float LONGITUDE_BOUND = 0.015f;
    private static LatLngBounds latLngBounds;

    //vars
    private boolean mLocationPermissionGranted = false; //used in permissions
    private GoogleMap mMap; //used to create the map
    private FusedLocationProviderClient mFusedLocationProviderClient; //used to get the location of the current user
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;

    //Retrofit usage
    private LatLngForRetrofit myPosition;

    //App Local Database
    private AppDatabase mDb;
    private MainViewModel mainViewModel;
    private List<RestaurantEntry> restaurants;

    //Firebase
    private FirebaseDatabase fireDb;
    private DatabaseReference fireDbRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean("boolean", true);
//        editor.apply();

        mDb = AppDatabase.getInstance(getApplicationContext());
        fireDb = FirebaseDatabase.getInstance();
        fireDbRef = fireDb.getReference("users");

        // TODO: 24/05/2018 Create a list with all the users of the App doesn't make much sense
        listOfUsers = new ArrayList<>();

        //---------------------- CODE FIRST WRITTEN --------------------------//

        navigationView = findViewById(R.id.bottom_navigation_id);
        navigationView.setOnNavigationItemSelectedListener(botNavListener);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_id);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view_id);
        mNavigationView.setNavigationItemSelectedListener(navViewListener);

        View headerView = mNavigationView.getHeaderView(0);
        TextView navUserName = (TextView) headerView.findViewById(R.id.nav_drawer_name_id);
        TextView navUserEmail = (TextView) headerView.findViewById(R.id.nav_drawer_email_id);

        /** We get the user information
         * */
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            name = auth.getCurrentUser().getDisplayName();
            email = auth.getCurrentUser().getEmail();

            // TODO: 24/05/2018 Save this to the database (firebase)
            /** This listener is supposedly only called once and then removed. With it,
             * we fill the listOfUsers and check if the user is already in the database.
             * If the user is not, we add him/her to the database
             * */
            fireDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    User user;

                    if (dataSnapshot.getChildren() != null) {
                        Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                        Log.d(TAG, "onDataChange: KEY = " + dataSnapshot.getKey());

                        for (DataSnapshot item: dataSnapshot.getChildren()) {
                            Log.d(TAG, "onDataChange: DATASNAPSHOT = " + item.toString());

                            user = new User(
                                    item.child(StringValues.FirebaseReference.FIRSTNAME).toString(),
                                    item.child(StringValues.FirebaseReference.LASTNAME).toString(),
                                    item.child(StringValues.FirebaseReference.EMAIL).toString(),
                                    item.child(StringValues.FirebaseReference.GROUP).toString(),
                                    item.child(StringValues.FirebaseReference.PLACE_ID).getValue().toString(),
                                    item.child(StringValues.FirebaseReference.RESTAURANT).toString(),
                                    item.child(StringValues.FirebaseReference.RESTAURANT_TYPE).toString()
                            );

                            listOfUsers.add(user);
                        }
                    }

                    for (int i = 0; i < listOfUsers.size() ; i++) {

                        if (listOfUsers.get(i).getEmail().equals(auth.getCurrentUser().getEmail())) {
                            //if the user is already in the database, do nothing
                        } else {
                            //if the user is not in the database, add him

                            String name = auth.getCurrentUser().getDisplayName();

                            if (name != null) {

                                String[] nameParts = name.split(" ");
                                fireDbRef.push().setValue(new User(
                                        nameParts[0],
                                        nameParts[1],
                                        auth.getCurrentUser().getEmail(),
                                        "None",
                                        "None",
                                        "None",
                                        "None"
                                ));
                            }

                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }

        navUserName.setText(name);
        navUserEmail.setText(email);

        //---------------------- GET CURRENT LOCATION --------------------------//

        if (isServicesOK()) {

            //getLocationPermission() calls getDeviceLocation(). We can then store the Device Location
            //in the database and use it in FragmentRestaurantMapView to display the current location
            getLocationPermission();

        }

        // TODO: 21/05/2018 The fragment has to start working after 3,4 seconds.
        // TODO: 21/05/2018 Create a progress bar that hides everything and, when loaded, make it disappear
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_id, FragmentRestaurantMapViewTRIAL.newInstance())
                .commit();

        /** Use header view to change name displayed in navigation drawer
         NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
         View headerView = navigationView.getHeaderView(0);
         TextView navUsername = (TextView) headerView.findViewById(R.id.navUsername);
         navUsername.setText("Your Text Here");
         * */

    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*****************
     * LISTENERS *****
     * **************/

    private BottomNavigationView.OnNavigationItemSelectedListener botNavListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {

                        // TODO: 20/05/2018 If the user is currently in the fragment, avoid relaunching it (use a flag for example).
                        case R.id.nav_view_map_id:
                            selectedFragment = FragmentRestaurantMapViewTRIAL.newInstance();
                            break;
                        case R.id.nav_view_list_id:
                            selectedFragment = FragmentRestaurantListViewTRIAL.newInstance();
                            break;
                        case R.id.nav_view_coworkers_id:
                            selectedFragment = FragmentCoworkersView.newInstance();
                            break;
                    }

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container_id, selectedFragment)
                            .commit();

                    //true means that we want to select the clicked item
                    //if we choose false, the fragment will be shown but the item
                    //won't be selected
                    return true;
                }
            };

    private NavigationView.OnNavigationItemSelectedListener navViewListener =
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    switch (item.getItemId()){

                        case R.id.nav_lunch: {
                            Log.d(TAG, "onNavigationItemSelected: lunch pressed");

                            // TODO: 19/05/2018 Put in an intent the title of the place you are going toR
                            startActivity(new Intent(MainActivity.this, RestaurantActivity.class));

                            return true;
                        }

                        case R.id.nav_settings: {
                            Log.d(TAG, "onNavigationItemSelected: settings pressed");

                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));

                            return true;
                        }

                        case R.id.nav_logout: {
                            Log.d(TAG, "onNavigationItemSelected: log out pressed");

                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            boolean notif = sharedPreferences.getBoolean(getResources().getString(R.string.pref_key_notifications), false);

                            ToastHelper.toastShort(MainActivity.this, "Notif pref = " + String.valueOf(notif));

                            auth.signOut();

                            startActivity(new Intent(MainActivity.this, AuthSignInActivity.class));
                            finish();

                            return true;
                        }

                    }

                    item.setChecked(true);

                    return true;
                }
            };

    /** Getter used to get the NavigationDrawer from inside a fragment
     * */
    public DrawerLayout getMDrawerLayout() {
        return mDrawerLayout;
    }



    // --------------------------------- NEW CODE ---------------------------------//

    /** Checks if the user has the correct Google Play Services Version
     */
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //Everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;

        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //There is an error but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();

        } else {
            Log.d(TAG, "isServicesOK: an error occurred; you cannot make map requests");
            ToastHelper.toastLong(MainActivity.this, "You can't make map requests");

        }
        return false;
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permission");

        /** We can also check first if the Android Version of the device is equal or higher than Marshmallow:
         *      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { "rest of code" } */


        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (ContextCompat.checkSelfPermission(
                MainActivity.this, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(
                   MainActivity.this, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;

            }

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, LOCATION_PERMISSION_REQUEST_CODE);

        }

        if (mLocationPermissionGranted) {
            getDeviceLocation();
        }
    }

    private void getDeviceLocation() {

        Log.d(TAG, "getDeviceLocation: getting device's location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        try {

            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if (task.isSuccessful() && task.getResult() != null) {
                        //&& task.getResult() != null -- allows you to avoid crash if the app
                        // did not get the location from the device (= currentLocation = null)
                        Log.d(TAG, "onComplete: found location!");
                        Location currentLocation = (Location) task.getResult();

                        LatLng northEast = new LatLng(currentLocation.getLatitude() + LATITUDE_BOUND, currentLocation.getLongitude() + LONGITUDE_BOUND);
                        LatLng southWest = new LatLng(currentLocation.getLatitude() - LATITUDE_BOUND, currentLocation.getLongitude() - LONGITUDE_BOUND);

                        Log.d(TAG, "onComplete: currentLocation: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                        Log.d(TAG, "onComplete: northEast: " + (currentLocation.getLatitude() + LATITUDE_BOUND) + ", " + (currentLocation.getLongitude() + LONGITUDE_BOUND));
                        Log.d(TAG, "onComplete: southWest: " + (currentLocation.getLatitude() - LATITUDE_BOUND) + ", " + (currentLocation.getLongitude() - LATITUDE_BOUND));

                        myPosition = new LatLngForRetrofit(currentLocation.getLatitude(), currentLocation.getLongitude());

                        latLngBounds = new LatLngBounds(
                                southWest, northEast);

                        // TODO: 25/05/2018 Uncomment this
                        //We delete the database
                        //AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        //    @Override
                        //    public void run() {
                        //        mDb.restaurantDao().deleteAllRowsInRestaurantTable();
                        //    }
                        //});

                        /** We do the API Requests. They will fill the database
                         * */
                        // TODO: 24/05/2018 Allow to do the calls when ready
                        //callLoaderInitApiGeneralRequests(ID_LOADER_INIT_GENERAL_API_REQUESTS);

                        //Log.d(TAG, "onComplete: current location: getLatitude(), getLongitude() " + (currentLocation.getLatitude()) + ", " + (currentLocation.getLongitude()));

                    } else {
                        Log.d(TAG, "onComplete: current location is null");
                    }

                }
            });

        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException " + e.getMessage());
        }

    }

    private void callLoaderInitApiGeneralRequests(int id) {

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<Void> loader = loaderManager.getLoader(id);

        if (loader == null) {
            Log.i(TAG, "loadLoaderUpdateSwitchTable: ");
            loaderManager.initLoader(id, null, loaderInitApiTextSearchRequests);
        } else {
            Log.i(TAG, "loadLoaderUpdateSwitchTable: ");
            loaderManager.restartLoader(id, null, loaderInitApiTextSearchRequests);
        }
    }


    /**********************/
    /** LOADER CALLBACKS **/
    /**********************/

    /** This LoaderCallback
     * uses ATLInitApi
     * */
    private LoaderManager.LoaderCallbacks loaderInitApiTextSearchRequests =
            new LoaderManager.LoaderCallbacks() {

                @Override
                public Loader onCreateLoader(int id, Bundle args) {
                    Log.d(TAG, "onCreateLoader: is called");
                    return new ATLInitApiTextSearchRequests(MainActivity.this, mDb, myPosition);
                }

                @Override
                public void onLoadFinished(Loader loader, Object data) {

                }

                @Override
                public void onLoaderReset(Loader loader) {

                }
            };
}

/**
 * android:id="@+id/nav_camera"
 android:icon="@drawable/ic_lunch"
 android:title="YOUR LUNCH" />
 <item
 android:id="@+id/nav_gallery"
 android:icon="@drawable/ic_settings"
 android:title="SETTINGS" />
 <item
 android:id="@+id/nav_slideshow"
 android:icon="@drawable/ic_logout"
 android:title="LOG OUT" />
 * **/