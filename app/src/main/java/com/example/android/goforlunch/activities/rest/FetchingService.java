package com.example.android.goforlunch.activities.rest;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.data.AppDatabase;
import com.example.android.goforlunch.data.AppExecutors;
import com.example.android.goforlunch.data.RestaurantEntry;
import com.example.android.goforlunch.helpermethods.Utils;
import com.example.android.goforlunch.helpermethods.UtilsRemote;
import com.example.android.goforlunch.remote.models.distancematrix.DistanceMatrix;
import com.example.android.goforlunch.remote.models.placebyid.PlaceById;
import com.example.android.goforlunch.remote.models.placebynearby.LatLngForRetrofit;
import com.example.android.goforlunch.remote.models.placebynearby.PlacesByNearby;
import com.example.android.goforlunch.remote.models.placebynearby.Result;
import com.example.android.goforlunch.remote.models.placetextsearch.PlacesByTextSearch;
import com.example.android.goforlunch.remote.remote.AllGoogleServices;
import com.example.android.goforlunch.remote.remote.GoogleService;
import com.example.android.goforlunch.remote.remote.GoogleServiceStreams;
import com.example.android.goforlunch.repository.RepoStrings;
import com.snatik.storage.Storage;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.android.goforlunch.repository.RepoStrings.Keys.NEARBY_KEY;

/**
 * Created by Diego Fajardo on 11/07/2018.
 */
public class FetchingService extends Service {

    private static final String TAG = FetchingService.class.getSimpleName();

    private Map<String,RestaurantEntry> mapOfRestaurants;
    private String[] arrayOfTypes;

    private LatLngForRetrofit myPosition;
    private Disposable disposable;

    private RestaurantEntry restaurantEntry;

    private boolean accessInternalStorageGranted = false;

    private AppDatabase localDatabase;

    private Storage internalStorage;
    private String mainPath;
    private String imageDirPath;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: called!");

        localDatabase = AppDatabase.getInstance(getApplicationContext());

        configuringInternalStorage();

        mapOfRestaurants = new HashMap<>();

        /* We pass the NON TRANSLATED ARRAY!
        * */
        arrayOfTypes = getResources().getStringArray(R.array.fetchTypesOfRestaurants);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called!");

        final int latitute = intent.getIntExtra("latitude", 0);
        final int longitude = intent.getIntExtra("longitude", 0);
        accessInternalStorageGranted = intent.getBooleanExtra("accessInternalStorage", false);

        if (latitute == 0 || longitude == 0) {
            //do nothing

        } else {

            myPosition = new LatLngForRetrofit(latitute,longitude);

            Utils.checkInternetInBackgroundThread(new DisposableObserver<Boolean>() {
                @Override
                public void onNext(Boolean aBoolean) {
                    Log.d(TAG, "onNext: " + aBoolean);

                    if (!aBoolean) {
                        //do nothing, there is no internet

                    } else {

                        startNearbyPlacesProcess();

                    }

                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "onError: " + e.getMessage());
                }

                @Override
                public void onComplete() {
                    Log.d(TAG, "onComplete: ");

                }
            });


        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: called!");
        return null;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called!");
        super.onDestroy();
    }

    private void startNearbyPlacesProcess () {
        Log.d(TAG, "startNearbyPlacesProcess: called!");

        /*1. We start fetching nearby places. */
        disposable =
                GoogleServiceStreams.streamFetchPlacesNearby(
                        myPosition,
                        "distance",
                        "restaurant",
                        NEARBY_KEY)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribeWith(new DisposableObserver<PlacesByNearby>() {
                            @Override
                            public void onNext(PlacesByNearby placesByNearby) {
                                Log.d(TAG, "onNext: called!");

                                List<Result> listOfResults = placesByNearby.getResults();

                                for (int i = 0; i < listOfResults.size(); i++) {

                                    mapOfRestaurants.put(
                                            Utils.checkToAvoidNull(listOfResults.get(i).getPlaceId()),
                                            new RestaurantEntry(
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getPlaceId()),
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getName()),
                                                    13,
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getVicinity()),
                                                    RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                    RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getRating()),
                                                    RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                    RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                    RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getGeometry().getLocation().getLat().toString()),
                                                    Utils.checkToAvoidNull(listOfResults.get(i).getGeometry().getLocation().getLng().toString()))
                                    );

                                }

                                /* Fetching nearby places has ended,
                                we start Text Search Process
                                */

                                Log.i(TAG, "onNext: NEARBY PLACES PROCESS ENDED!");

                                for (int i = 1; i < arrayOfTypes.length - 1; i++) { //-1 because we don't want to fetch "type OTHER" restaurants

                                    startTextSearchProcess(i);

                                }

                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: " + e.getMessage());
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: called!");

                            }
                        });

    }



    private void startTextSearchProcess(final int type) {
        Log.d(TAG, "startTextSearchProcess: called!");

        disposable =
                GoogleServiceStreams.streamFetchPlacesTextSearch(
                        arrayOfTypes[type] + "+" + "Restaurant",
                        myPosition,
                        20,
                        RepoStrings.Keys.TEXTSEARCH_KEY)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribeWith(new DisposableObserver<PlacesByTextSearch>() {
                            @Override
                            public void onNext(PlacesByTextSearch placesByTextSearch) {
                                Log.d(TAG, "onNext: ");

                                List<com.example.android.goforlunch.remote.models.placetextsearch.Result> listOfResults = placesByTextSearch.getResults();

                                for (int i = 0; i < listOfResults.size(); i++) {

                                    if (mapOfRestaurants.get(listOfResults.get(i).getPlaceId()) != null) {
                                        /* If the place is already in the map and the type is equal to 13,
                                        we only update the type */

                                        restaurantEntry = mapOfRestaurants.get(listOfResults.get(i).getPlaceId());

                                        if (restaurantEntry.getType() == 13) {
                                            restaurantEntry.setType(type);
                                            mapOfRestaurants.put(restaurantEntry.getPlaceId(), restaurantEntry);
                                        }

                                    } else {
                                        /* If the place is not already in the map,
                                        we add the restaurant to it */

                                        mapOfRestaurants.put(
                                                Utils.checkToAvoidNull(listOfResults.get(i).getPlaceId()),
                                                new RestaurantEntry(
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getPlaceId()),
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getName()),
                                                        13,
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getFormattedAddress()),
                                                        RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                        RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getRating()),
                                                        RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                        RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                        RepoStrings.NOT_AVAILABLE_FOR_STRINGS,
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getGeometry().getLocation().getLat().toString()),
                                                        Utils.checkToAvoidNull(listOfResults.get(i).getGeometry().getLocation().getLng().toString()))
                                        );

                                    }
                                }

                                if (type == 12) {
                                    /* if type is 12, then is type = Vietnamese which is the last one (before Other)
                                    This guarantees all restaurants are already in the map. We can proceed with getting placeId information
                                    to update Distance and Photos */

                                    Log.i(TAG, "onNext: TEXT SEARCH PROCESS ENDED!");

                                    startPlaceIdProcess();

                                }

                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: " + e.getMessage());

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: ");

                            }
                        });

    }

    private void startPlaceIdProcess () {
        Log.d(TAG, "startPlaceIdProcess: called!");

        /* We iterate through the map
        * */

        Log.i(TAG, "startPlaceIdProcess: PLACE ID PROCESS STARTED");

        Iterator<Map.Entry<String, RestaurantEntry>> iter = mapOfRestaurants.entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry<String, RestaurantEntry> restaurantEntry = iter.next();

            /* We update each place with the PlaceId info
            * */
            updateMapWithPlaceIdInfo(restaurantEntry);

        }

        Log.i(TAG, "startPlaceIdProcess: PLACE ID PROCESS ENDED");
        Log.i(TAG, "startPlaceIdProcess: DISTANCE MATRIX PROCESS STARTED");

        /* We have reached the end of the loop, so we can start with
         * Distance Matrix process */
        iter = mapOfRestaurants.entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry<String, RestaurantEntry> restaurantEntry = iter.next();

            /* We update each place with the PlaceId info
             * */
            updateMapWithDistanceMatrix(restaurantEntry);

        }


        Log.i(TAG, "startPlaceIdProcess: DISTANCE MATRIX PROCESS ENDED!");
        Log.i(TAG, "startPlaceIdProcess: PHOTO PROCESS STARTED");

        /* We have reached the end of the loop, so we can start with
         * Photo process */
        iter = mapOfRestaurants.entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry<String, RestaurantEntry> restaurantEntry = iter.next();

            /* We update each place with the PlaceId info
             * */
            updateMapAndStorageWithPhotos(restaurantEntry);

        }

        Log.i(TAG, "startPlaceIdProcess: PHOTO PROCESS PROCESS ENDED!");


    }

    private void updateMapWithPlaceIdInfo(final Map.Entry<String,RestaurantEntry> restaurantEntry) {
        Log.d(TAG, "updateMapWithPlaceIdInfo: called!");

        disposable = GoogleServiceStreams.streamFetchPlaceById(
                restaurantEntry.getValue().getPlaceId(),
                RepoStrings.Keys.PLACEID_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableObserver<PlaceById>() {
                    @Override
                    public void onNext(PlaceById placeById) {
                        Log.d(TAG, "onNext: ");

                        com.example.android.goforlunch.remote.models.placebyid.Result result =
                                placeById.getResult();

                        if (result != null) {

                            String closingTime = UtilsRemote.checkClosingTime(result);

                            restaurantEntry.getValue().setOpenUntil(closingTime);
                            restaurantEntry.getValue().setPhone(Utils.checkToAvoidNull(result.getInternationalPhoneNumber()));
                            restaurantEntry.getValue().setWebsiteUrl(Utils.checkToAvoidNull(result.getWebsite()));
                            restaurantEntry.getValue().setImageUrl(Utils.checkToAvoidNull(result.getPhotos().get(0).getPhotoReference()));

                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }


                });
    }

    private void updateMapWithDistanceMatrix (final Map.Entry<String,RestaurantEntry> restaurantEntry) {
        Log.d(TAG, "updateMapWithDistanceMatrix: called!");

        disposable =
                GoogleServiceStreams.streamFetchDistanceMatrix(
                        "imperial",
                        "place_id:" + restaurantEntry.getValue().getPlaceId(),
                        myPosition,
                        RepoStrings.Keys.MATRIX_DISTANCE_KEY)
                        .subscribeWith(new DisposableObserver<DistanceMatrix>() {
                            @Override
                            public void onNext(DistanceMatrix distanceMatrix) {
                                Log.d(TAG, "onNext: ");

                                if (distanceMatrix != null) {
                                    if (distanceMatrix.getRows() != null) {
                                        if (distanceMatrix.getRows().get(0) != null) {
                                            if (distanceMatrix.getRows().get(0).getElements() != null) {
                                                if (distanceMatrix.getRows().get(0).getElements().get(0) != null) {
                                                    if (distanceMatrix.getRows().get(0).getElements().get(0).getDistance() != null) {
                                                        if (distanceMatrix.getRows().get(0).getElements().get(0).getDistance().getText() != null) {
                                                            restaurantEntry.getValue().setDistance(distanceMatrix.getRows().get(0).getElements().get(0).getDistance().getText());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: " + e.getMessage() );



                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: ");



                            }
                        });

    }

    private void updateMapAndStorageWithPhotos (final Map.Entry<String, RestaurantEntry> restaurantEntry) {
        Log.d(TAG, "updateMapAndStorageWithPhotos: called!");

        GoogleService googleService = AllGoogleServices.getGooglePlacePhotoService();
        Call<ResponseBody> callPhoto = googleService.fetchDataPhoto( "400",
               restaurantEntry.getValue().getImageUrl(),
                RepoStrings.Keys.PHOTO_KEY);
        callPhoto.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(final Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: PHOTO: url = " + call.request().url().toString());
                Log.d(TAG, "onResponse: PHOTO: response = " + response.toString());

                /* We save the image url in the database
                 * */
                Log.d(TAG, "onResponse: PHOTO: saving url in database");
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: saving url...");

                        restaurantEntry.getValue().setImageUrl(call.request().url().toString());

                    }
                });

                Log.d(TAG, "onResponse: PHOTO: accessInternalStorage");

                /* We store the image in the internal storage if the access is granted
                 * */
                Log.d(TAG, "onResponse: PHOTO saving image in storage");

                if (response.body() != null) {
                    Log.d(TAG, "onResponse: PHOTO response.body() IS NOT NULL");

                    Bitmap bm = BitmapFactory.decodeStream(response.body().byteStream());

                    if (restaurantEntry.getValue().getPlaceId() != null && bm != null) {
                        saveImageInInternalStorage(restaurantEntry.getValue().getPlaceId(), bm);
                    }

                } else {
                    Log.d(TAG, "onResponse: response.body() is null");
                }

            }

            @Override
            public void onFailure(final Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: url = " + call.request().url().toString());

                /* We also save the url if onFailure is called
                 * */
                saveUrlInLocalDatabase(restaurantEntry.getValue().getPlaceId(), call.request().url().toString() );

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: saving url...");
                        localDatabase.restaurantDao()
                                .updateRestaurantImageUrl(restaurantEntry.getValue().getPlaceId(), call.request().url().toString());

                    }
                });

            }
        });
    }

    /** This method saves the fetched image in the internal storage
     * */
    private void saveImageInInternalStorage (String placeId, final Bitmap bitmap) {
        Log.d(TAG, "saveImageInInternalStorage: called!");

        if (accessInternalStorageGranted) {

            if (internalStorage.isDirectoryExists(imageDirPath)) {

                final String filePath = imageDirPath + placeId;

                if (bitmap != null) {

                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: Saving file...");
                            boolean isFileCreated = internalStorage.createFile(filePath, bitmap);
                            Log.d(TAG, "run: file saved = " + isFileCreated);

                        }
                    });

                }

            }


        } else {
            Log.i(TAG, "saveImageInInternalStorage: accessInternalStorageGrantes = false");
        }

    }

    /** This method stores the image url in the local database
     * */
    private void saveUrlInLocalDatabase (final String placeId, final String url) {
        Log.d(TAG, "storeUrlInLocalDatabase: called!");

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: saving url...");
                localDatabase.restaurantDao()
                        .updateRestaurantImageUrl(placeId, url);

            }
        });

    }


    /** This method destroys and recreates the internal directory where all the images are stored.
     * The objective of this process is to delete all old images to free memory space
     * */
    private void configuringInternalStorage () {
        Log.d(TAG, "configuringInternalStorage: called!");

        internalStorage = new Storage(getApplicationContext());
        mainPath = internalStorage.getInternalFilesDirectory() + File.separator;
        imageDirPath = mainPath + File.separator + RepoStrings.Directories.IMAGE_DIR + File.separator;

        /* We delete the directory to delete all the information
        * */
        boolean isDeleted = internalStorage.deleteDirectory(imageDirPath);
        Log.i(TAG, "configuringInternalStorage: isDeleted = " + isDeleted);

        /* We create it again
        * */
        String newDirectory = imageDirPath;
        boolean isCreated = internalStorage.createDirectory(newDirectory);
        Log.d(TAG, "onClick: isCreated = " + isCreated);

    }

}

