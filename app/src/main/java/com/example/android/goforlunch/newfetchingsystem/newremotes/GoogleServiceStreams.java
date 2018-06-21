package com.example.android.goforlunch.newfetchingsystem.newremotes;

import com.example.android.goforlunch.newfetchingsystem.newmodels.distancematrix.DistanceMatrix;
import com.example.android.goforlunch.newfetchingsystem.newmodels.placebyid.PlaceById;
import com.example.android.goforlunch.newfetchingsystem.newmodels.placebynearby.LatLngForRetrofit;
import com.example.android.goforlunch.newfetchingsystem.newmodels.placebynearby.PlacesByNearby;
import com.example.android.goforlunch.newfetchingsystem.newmodels.placetextsearch.PlacesByTextSearch;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Diego Fajardo on 19/06/2018.
 */
public class GoogleServiceStreams {

    public static Observable<PlacesByNearby> streamFetchPlacesNearby(
            LatLngForRetrofit latLngForRetrofit,
            String rankBy,
            String type,
            String key) {

        GoogleService googleService = AllGoogleServices.getGoogleNearbyService();

        return googleService.fetchDataNearby(latLngForRetrofit, rankBy, type, key)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);

    }

    public static Observable<PlacesByTextSearch> streamFetchPlacesTextSearch(
            String query,
            LatLngForRetrofit latLngForRetrofit,
            int radius,
            String key) {

        GoogleService googleService = AllGoogleServices.getGooglePlaceTextSearchService();

        return googleService.fetchDataTextSearch(query, latLngForRetrofit, radius, key)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);

//        Can be simplified into
//        return AllGoogleServices.getGooglePlaceTextSearchService()
//                .fetchDataTextSearch(query, latLngForRetrofit, radius, key)
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .timeout(10, TimeUnit.SECONDS);
//        but we leave it like it is for code readability

    }

    public static Observable<PlaceById> streamFetchPlaceById(
            String placeId,
            String key) {

        GoogleService googleService = AllGoogleServices.getGooglePlaceIdService();

        return googleService.fetchDataPlaceId(placeId, key)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);

    }

    public static Observable<DistanceMatrix> streamFetchDistanceMatrix(
            String units,
            String placeId,
            LatLngForRetrofit latLngForRetrofit,
            String key) {

        GoogleService googleService = AllGoogleServices.getGoogleDistanceMatrixService();

        return googleService.fetchDistanceMatrix(units, placeId, latLngForRetrofit, key)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);
    }

//    public static Observable<String> streamFetchPhoto(
//            String maxWidth,
//            String photoReference,
//            String key) {
//
//        GoogleService googleService = AllGoogleServices.getGooglePlacePhotoService();
//
//        return googleService.fetchDataPhoto(maxWidth, photoReference, key)
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .timeout(10, TimeUnit.SECONDS);
//
//    }

}




