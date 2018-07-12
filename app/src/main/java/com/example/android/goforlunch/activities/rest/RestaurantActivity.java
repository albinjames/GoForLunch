package com.example.android.goforlunch.activities.rest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.example.android.goforlunch.R;
import com.example.android.goforlunch.data.RestaurantEntry;
import com.example.android.goforlunch.helpermethods.Anim;
import com.example.android.goforlunch.helpermethods.ToastHelper;
import com.example.android.goforlunch.helpermethods.Utils;
import com.example.android.goforlunch.helpermethods.UtilsFirebase;
import com.example.android.goforlunch.recyclerviewadapter.RVAdapterList;
import com.example.android.goforlunch.recyclerviewadapter.RVAdapterRestaurant;
import com.example.android.goforlunch.repository.RepoStrings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.snatik.storage.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Diego Fajardo on 06/05/2018.
 */

public class RestaurantActivity extends AppCompatActivity {

    private static final String TAG = RestaurantActivity.class.getSimpleName();

    private Context context;

    //Widgets
    @BindView(R.id.restaurant_fab_id)
    FloatingActionButton fab;

    @BindView(R.id.restaurant_selector_id)
    BottomNavigationView navigationView;

    @BindView(R.id.restaurant_image_id)
    ImageView ivRestPicture;

    @BindView(R.id.restaurant_title_id)
    TextView tvRestName;

    @BindView(R.id.restaurant_address_id)
    TextView tvRestAddress;

    @BindView(R.id.restaurant_rating_id)
    RatingBar rbRestRating;

    //Variables
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String userKey;
    private String userGroup;
    private String userGroupKey;
    private String userRestaurant;
    private String intentRestaurantName;

    private boolean fabShowsCheck;
    private String phoneToastString = "No phone available";
    private String webUrlToastString = "No web available";
    private String likeToastString = "Liked!";

    private List<String> listOfCoworkers;

    //RecyclerView
    @BindView(R.id.restaurant_recycler_view_id)
    RecyclerView recyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Firebase Database
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseDatabase fireDb;
    private DatabaseReference dbRefUsers;
    private DatabaseReference dbRefGroups;

    private SharedPreferences sharedPref;

    //Glide
    private RequestManager glide;

    //Internal Storage
    private Storage storage;
    private String mainPath;
    private String imageDirPath;
    private boolean accessToInternalStorageGranted = false;

    // Disposable
    private Disposable getImageFromInternalStorageDisposable;

    //Intent
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);

        ButterKnife.bind(this);

        context = RestaurantActivity.this;

        fireDb = FirebaseDatabase.getInstance();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        glide = Glide.with(context);

        userKey = sharedPref.getString(RepoStrings.SharedPreferences.USER_ID_KEY, "");

        /** Instantiation of the fab and set onClick listener*/
        fab.setOnClickListener(mFabListener);

        listOfCoworkers = new ArrayList<>();

        navigationView.setOnNavigationItemSelectedListener(bottomViewListener);

        this.configureRecyclerView();
        this.configureInternalStorage(context);

        /** We get the intent to display the information
         * */
        intent = getIntent();
        fillUIUsingIntent(intent);
        intentRestaurantName = intent.getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME);

        /** We get the user information
         * */
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        Log.d(TAG, "onDataChange... auth.getCurrentUser() = " + (auth.getCurrentUser() != null));

        if (currentUser != null) {

            userEmail = currentUser.getEmail();

            if (userEmail != null && !userEmail.equalsIgnoreCase("")) {

                dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS + "/" + userKey);
                dbRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: " + dataSnapshot.toString());

                        userFirstName = dataSnapshot.child(RepoStrings.FirebaseReference.USER_FIRST_NAME).getValue().toString();
                        userLastName = dataSnapshot.child(RepoStrings.FirebaseReference.USER_LAST_NAME).getValue().toString();
                        userEmail = dataSnapshot.child(RepoStrings.FirebaseReference.USER_EMAIL).getValue().toString();
                        userGroup = dataSnapshot.child(RepoStrings.FirebaseReference.USER_GROUP).getValue().toString();
                        userGroupKey = dataSnapshot.child(RepoStrings.FirebaseReference.USER_GROUP_KEY).getValue().toString();
                        userRestaurant = dataSnapshot.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                                .child(RepoStrings.FirebaseReference.RESTAURANT_NAME).getValue().toString();

                        setFabButtonState(intentRestaurantName);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled: " + databaseError.getCode());
                    }
                });

            }
        }

        /** Reference to Firebase Database, users.
         * We get the list of coworkers that will go to this Restaurant which
         * will be displayed in the recyclerView
         * */
        dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
        dbRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: " + dataSnapshot.toString());

                listOfCoworkers = UtilsFirebase.fillListWithCoworkersOfSameGroupAndSameRestaurantExceptIfItsTheUser(dataSnapshot, userEmail, userGroup, intentRestaurantName);

                /** We use the list in the adapter
                 * */
                mAdapter = new RVAdapterRestaurant(context, listOfCoworkers);
                recyclerView.setAdapter(mAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.getCode());

            }
        });

        Anim.crossFadeShortAnimation(recyclerView);

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called!");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called!");
        super.onStop();
    }

    /** disposeWhenDestroy() avoids memory leaks
     * */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called!");
        super.onDestroy();
        this.disposeWhenDestroy();
    }

    private void disposeWhenDestroy () {
        Utils.dispose(this.getImageFromInternalStorageDisposable);

    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    /*****************
     * LISTENERS *****
     * **************/

    private View.OnClickListener mFabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Utils.checkInternetInBackgroundThread(new DisposableObserver<Boolean>() {
                @Override
                public void onNext(Boolean aBoolean) {
                    Log.d(TAG, "onNext: " + aBoolean);

                    if (aBoolean) {
                        Log.d(TAG, "onNext: internet state = " + aBoolean);

                        Map<String,Object> map;

                        if (fabShowsCheck) {
                            /** If we click the fab when it shows check it has to display "add".
                             * Moreover, we modify the info in the database
                             * */
                            fabShowsCheck = false;
                            Log.d(TAG, "onClick: fabShowsCheck = " + fabShowsCheck);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add, getApplicationContext().getTheme()));

                                /** We delete the restaurant from the database (user's)
                                 **/
                                dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS + "/" + userKey + "/" + RepoStrings.FirebaseReference.USER_RESTAURANT_INFO);
                                UtilsFirebase.deleteRestaurantInfoOfUserInFirebase(dbRefUsers);

                                ToastHelper.toastShort(context, getResources().getString(R.string.restaurantNotGoing));
                            }

                        } else {

                            /** If we click the fab when it shows "add" it has to display "check".
                             * Moreover, we modify the info in the database
                             * */
                            fabShowsCheck = true;
                            Log.d(TAG, "onClick: fabShowsCheck = " + fabShowsCheck);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_check, getApplicationContext().getTheme()));

                                /** We add the restaurant to the database (user's)
                                 * */
                                dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS + "/" + userKey + "/" + RepoStrings.FirebaseReference.USER_RESTAURANT_INFO);
                                UtilsFirebase.updateRestaurantsUserInfoInFirebase(dbRefUsers,
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.ADDRESS)),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.IMAGE_URL)),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.PHONE)),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.PLACE_ID)),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.RATING)),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME)),
                                        getIntent().getIntExtra(RepoStrings.SentIntent.RESTAURANT_TYPE, 0),
                                        Utils.checkIfIsNull(getIntent().getStringExtra(RepoStrings.SentIntent.WEBSITE_URL))
                                );

                                ToastHelper.toastShort(context, getResources().getString(R.string.restaurantGoing) + " " + intent.getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME) + "!");
                            }
                        }

                    } else {
                        Log.d(TAG, "onNext: internet state = " + aBoolean);
                        ToastHelper.toastShort(RestaurantActivity.this, getResources().getString(R.string.noInternet));

                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.d(TAG, "onError: " + Log.getStackTraceString(e));

                }

                @Override
                public void onComplete() {
                    Log.d(TAG, "onComplete: ");

                }
            });


        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener bottomViewListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    switch (item.getItemId()) {

                        case R.id.restaurant_view_call_id: {
                            Log.d(TAG, "onNavigationItemSelected: callButton CLICKED!");
                            Log.d(TAG, "onNavigationItemSelected: phone = " + phoneToastString);
                            if (phoneToastString.equals("")) {
                                ToastHelper.toastShort(context, getResources().getString(R.string.restaurantPhoneNotAvailable));
                            } else {
                                ToastHelper.toastShort(context, getResources().getString(R.string.restaurantCallingTo) + " " + phoneToastString);
                            }

                        } break;

                        case R.id.restaurant_view_like_id: {
                            Log.d(TAG, "onNavigationItemSelected: likeButton CLICKED!");

                            ToastHelper.toastShort(context, likeToastString);


                        } break;

                        case R.id.restaurant_view_website_id: {
                            Log.d(TAG, "onNavigationItemSelected: websiteButton CLICKED!");
                            Log.d(TAG, "onNavigationItemSelected: web URL = " + webUrlToastString);
                            if (webUrlToastString.equals("")) {
                                ToastHelper.toastShort(context, getResources().getString(R.string.restaurantWebsiteNotAvailable));
                            } else {

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrlToastString));
                                startActivity(browserIntent);

                            }

                        } break;

                    }

                    return false;
                }
            };

    /******************************************************
     * CONFIGURATION
     *****************************************************/


    private void configureRecyclerView () {

        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);

    }

    /******************************************************
     * RX JAVA
     *****************************************************/

    /** Method used to set the Fab button state
     * */
    private boolean setFabButtonState (String intentRestaurantName) {

        // TODO: 28/05/2018 See another way of doing things for lower versions
        if (userRestaurant.equals(intentRestaurantName)) {
            fabShowsCheck = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_check, getApplicationContext().getTheme()));
            }

        } else {
            fabShowsCheck = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add, getApplicationContext().getTheme()));
            }
        }

        return true;

    }

    /** Method that sets the directory variables and creates the directory that will
     * store images if needed
     * */
    private void configureInternalStorage (Context context) {
        Log.d(TAG, "configureInternalStorage: ");

        //If we don't have storage permissions, we don't continue
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ToastHelper.toastShort(context, getResources().getString(R.string.storageNotGranted));
            return;

        } else {
            Log.d(TAG, "configureInternalStorage: access to Internal storage granted");
            accessToInternalStorageGranted = true;
            storage = new Storage(context);
            mainPath = storage.getInternalFilesDirectory() + File.separator;
            imageDirPath = mainPath + RepoStrings.Directories.IMAGE_DIR + File.separator;

            Log.d(TAG, "configureInternalStorage: mainPath = " + mainPath);
            Log.d(TAG, "configureInternalStorage: imageDirPath = " + imageDirPath);

            ToastHelper.toastShort(context, getResources().getString(R.string.storageGranted));

        }
    }



    /** Method used to fill the UI using the intent
     * */
    private boolean fillUIUsingIntent(Intent intent) {

        if (intent.getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME) == null
                || intent.getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME).equals("")) {

            tvRestName.setText("Restaurant name not available");

        } else {

            StringBuilder displayedName = new StringBuilder();
            String tokens[] = intent.getStringExtra(RepoStrings.SentIntent.RESTAURANT_NAME).split(" ");

            for (int i = 0; i < tokens.length; i++) {
                if (displayedName.length() < 27) {

                    /** 1 is the space between words
                     * */
                    if ((displayedName.length() + tokens[i].length()) + 1 < 27) {
                        displayedName.append(" ").append(tokens[i]);

                    } else {
                        break;
                    }
                }
            }

            String transformedName = displayedName.toString().trim();

            tvRestName.setText(transformedName);
        }

        if (intent.getStringExtra(RepoStrings.SentIntent.ADDRESS) == null
                || intent.getStringExtra(RepoStrings.SentIntent.ADDRESS).equals("")) {

            tvRestAddress.setText("Address not available");

        } else {

            tvRestAddress.setText(intent.getStringExtra(RepoStrings.SentIntent.ADDRESS));
        }

        if (intent.getStringExtra(RepoStrings.SentIntent.RATING) == null
                || intent.getStringExtra(RepoStrings.SentIntent.RATING).equals("")) {

            rbRestRating.setRating(0f);

        } else {

            float rating = Float.parseFloat(intent.getStringExtra(RepoStrings.SentIntent.RATING));
            rbRestRating.setRating(rating);
        }

        phoneToastString = intent.getStringExtra(RepoStrings.SentIntent.PHONE);
        webUrlToastString = intent.getStringExtra(RepoStrings.SentIntent.WEBSITE_URL);

        if (accessToInternalStorageGranted) {
            loadImage(intent);

        } else {
            loadImageWithUrl(intent);

        }

        return true;
    }

    /** Method that tries to load an image using the storage.
     * If there is no file, it tries to load
     * the image with the url
     * */
    private void loadImage (Intent intent) {
        Log.d(TAG, "loadImage: called!");

        //if file exists in the directory -> load with storage
        if (storage.isFileExist(
                imageDirPath + intent.getStringExtra(RepoStrings.SentIntent.PLACE_ID))) {
            Log.d(TAG, "loadImage: file does exist in the directory");
            getAndDisplayImageFromInternalStorage(intent.getStringExtra(RepoStrings.SentIntent.PLACE_ID));

        } else {
            Log.d(TAG, "loadImage: file does not exist in the directory");
            loadImageWithUrl(intent);


        }
    }

    /** Method that tries to load an image with a url.
     * If it is null or equal to "", it loads
     * an standard picture
     * */
    private void loadImageWithUrl (Intent intent) {
        Log.d(TAG, "loadImageWithUrl: called!");

        Log.i(TAG, "loadImageWithUrl: " + intent.getStringExtra(RepoStrings.SentIntent.IMAGE_URL));

        if (intent.getStringExtra(RepoStrings.SentIntent.IMAGE_URL) == null
                || intent.getStringExtra(RepoStrings.SentIntent.IMAGE_URL).equals("")) {
            Log.d(TAG, "loadImageWithUrl: image is null");

            glide.load(R.drawable.lunch_image).into(ivRestPicture);

        } else {
            Log.d(TAG, "loadImageWithUrl: image is not null or empty");

            glide.load(intent.getStringExtra(RepoStrings.SentIntent.IMAGE_URL)).into(ivRestPicture);

        }
    }

    /** Used to read an image from the internal storage and convert it to bitmap so that
     * it the image can be stored in a RestaurantEntry and be displayed later using glide
     * in the recyclerView
     * */
    private Observable<byte[]> getObservableImageFromInternalStorage (String filePath) {
        return Observable.just(storage.readFile(filePath));
    }

    /** Loads an image using glide. The observable emits the image in a background thread
     * and the image is loaded using glide in the main thread
     * */
    public void getAndDisplayImageFromInternalStorage(String filePath) {
        Log.d(TAG, "loadImageFromInternalStorage: called!");

        getImageFromInternalStorageDisposable = getObservableImageFromInternalStorage(filePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<byte[]>() {
                    @Override
                    public void onNext(byte[] bytes) {
                        Log.d(TAG, "onNext: ");

                        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);
                        glide.load(bm).into(ivRestPicture);

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + Log.getStackTraceString(e));

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");

                    }
                });
    }

    /** Note that if you set an image via ImageView.setImageBitmap(BITMAP) it internally creates
     * a new BitmapDrawableeven if you pass null.
     * In that case the check imageViewOne.getDrawable() == null is false anytime.
     * To get to know if an image is set you can do the following
     * */
    // TODO: 12/07/2018 Move to Utils
    private boolean hasImage(@NonNull ImageView view) {
        Drawable drawable = view.getDrawable();
        boolean hasImage = (drawable != null);

        if (hasImage && (drawable instanceof BitmapDrawable)) {
            hasImage = ((BitmapDrawable)drawable).getBitmap() != null;
        }

        return hasImage;
    }

}
