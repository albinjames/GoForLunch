package com.example.android.goforlunch.helpermethods;

import android.util.Log;

import com.example.android.goforlunch.data.RestaurantEntry;
import com.example.android.goforlunch.pojo.User;
import com.example.android.goforlunch.repostrings.RepoStrings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Diego Fajardo on 14/06/2018.
 */



public class UtilsFirebase {

    private static final String TAG = "UtilsFirebase";



    /**
     * Method that deletes
     * all the restaurant info from a user in Firebase
     * */
    public static boolean deleteRestaurantInfoOfUserInFirebase(DatabaseReference dbRef) {
        // TODO: 28/05/2018 Take care, if sth is null it will be deleted from the database because it won't be added to the map

        Map<String, Object> map = new HashMap<>();
        map.put(RepoStrings.FirebaseReference.RESTAURANT_NAME, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_RATING, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_TYPE, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PHONE, "");
        map.put(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL, "");

        dbRef.updateChildren(map);
        return true;
    }

    /** Method that updates
     *  user's info in Firebase
     * */
    public static boolean updateUserInfoInFirebase (DatabaseReference dbRef,
                                                    String firstName,
                                                    String lastName,
                                                    String email,
                                                    String group,
                                                    String groupKey,
                                                    boolean notifications,
                                                    String userRestaurantInfo) {

        Map <String, Object> map = new HashMap<>();
        map.put(RepoStrings.FirebaseReference.USER_FIRST_NAME, firstName);
        map.put(RepoStrings.FirebaseReference.USER_LAST_NAME, lastName);
        map.put(RepoStrings.FirebaseReference.USER_EMAIL, email);
        map.put(RepoStrings.FirebaseReference.USER_GROUP, group);
        map.put(RepoStrings.FirebaseReference.USER_GROUP_KEY, groupKey);
        map.put(RepoStrings.FirebaseReference.USER_NOTIFICATIONS, notifications);
        map.put(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO, userRestaurantInfo);

        dbRef.updateChildren(map);
        return true;
    }

    /** Method that updates
     *  user's restaurant info in Firebase
     * */
    public static boolean updateRestaurantsUserInfoInFirebase (DatabaseReference dbRef,
                                                               String address,
                                                               String imageUrl,
                                                               String phone,
                                                               String placeId,
                                                               String rating,
                                                               String restaurantName,
                                                               String restaurantType,
                                                               String websiteUrl) {

        Map <String, Object> map = new HashMap<>();
        map.put(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS, address);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL, imageUrl);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PHONE, phone);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID, placeId);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_RATING, rating);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_NAME, restaurantName);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_TYPE, restaurantType);
        map.put(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL, websiteUrl);

        dbRef.updateChildren(map);
        return true;
    }

    /** Method that returns
     * all user restaurant info
     * */
    public static Map<String,Object> fillMapUsingDataSnapshot(DataSnapshot dataSnapshot) {

        Map <String, Object> map = new HashMap<>();

        map.put(RepoStrings.FirebaseReference.RESTAURANT_NAME, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_NAME).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_TYPE, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_TYPE).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_RATING, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_RATING).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PHONE, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_PHONE).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL).getValue().toString());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL, dataSnapshot.child(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL).getValue().toString());

        return map;
    }

    /** Method to fill a map with RestaurantEntry info
     * */
    public static Map<String, Object> fillMapUsingRestaurantEntry (RestaurantEntry restaurant) {

        Map<String, Object> map = new HashMap<>();

        map.put(RepoStrings.FirebaseReference.RESTAURANT_NAME, restaurant.getName());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_TYPE, restaurant.getType());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS, restaurant.getAddress());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_RATING, restaurant.getRating());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID, restaurant.getPlaceId());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_PHONE, restaurant.getPhone());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL, restaurant.getImageUrl());
        map.put(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL, restaurant.getWebsiteUrl());

        return map;
    }


    /**
     * Method that inserts
     * Restaurant info into user's info in Firebase
     * */
    public static boolean updateInfoWithMapInFirebase(DatabaseReference dbRef,
                                                      Map <String, Object> map) {
        dbRef.updateChildren(map);
        return true;
    }

    /**
     * Method that inserts
     * a new restaurant visited into the group
     * */
    public static boolean insertNewRestaurantInGroupInFirebase (DatabaseReference dbRef,
                                                                String restaurantName) {

        Map<String, Object> map = new HashMap<>();
        map.put(restaurantName, true);

        dbRef.updateChildren(map);
        return true;
    }

    /** Method to fill a list with all restaurants in a group
     * */
    public static List<String> fillListWithGroupRestaurantsUsingDataSnapshot (DataSnapshot dataSnapshot) {
        Log.d(TAG, "fillListWithGroupRestaurantsUsingDataSnapshot: called!");

        List<String> listOfRestaurants = new ArrayList<>();

        for (DataSnapshot item :
                dataSnapshot.getChildren()) {

            listOfRestaurants.add(item.getKey());
        }

        return listOfRestaurants;

    }

    /** Method to fill a list with all users of a specific group using a dataSnapshot
     * */
    public static List<User> fillListWithUsersFromDataSnapshot (DataSnapshot dataSnapshot, String email, String group) {

        List<User> listOfUsers = new ArrayList<>();

        for (DataSnapshot item :
                dataSnapshot.getChildren()) {

            if (item.child(RepoStrings.FirebaseReference.USER_GROUP).getValue().toString().equalsIgnoreCase(group)
                    && !item.child(RepoStrings.FirebaseReference.USER_EMAIL).getValue().toString().equalsIgnoreCase(email)) {

                User.Builder builder = new User.Builder();

                builder.setFirstName(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_FIRST_NAME).getValue()).toString());
                builder.setLastName(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_LAST_NAME).getValue()).toString());
                builder.setEmail(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_EMAIL).getValue()).toString());
                builder.setGroup(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_GROUP).getValue()).toString());

                builder.setAddress(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_ADDRESS).getValue()).toString());
                builder.setImageUrl(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_IMAGE_URL).getValue()).toString());
                builder.setPhone(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_PHONE).getValue()).toString());
                builder.setPlaceId(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_PLACE_ID).getValue()).toString());
                builder.setRating(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_RATING).getValue()).toString());
                builder.setRestaurantName(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_NAME).getValue()).toString());
                builder.setRestaurantType(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_TYPE).getValue()).toString());
                builder.setWebsiteUrl(Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO)
                        .child(RepoStrings.FirebaseReference.RESTAURANT_WEBSITE_URL).getValue()).toString());

                listOfUsers.add(builder.create());
            }
        }

        return listOfUsers;

    }

    public static List<String> fillListWithCoworkersOfSameGroupAndSameRestaurant (DataSnapshot dataSnapshot, String userGroup, String intentRestaurant) {

        List<String> listOfCoworkers = new ArrayList<>();

        for (DataSnapshot item :
                dataSnapshot.getChildren()) {

            if (item.child(RepoStrings.FirebaseReference.USER_GROUP).getValue().toString().equalsIgnoreCase(userGroup)
                    && item.child(RepoStrings.FirebaseReference.USER_RESTAURANT_INFO).child(RepoStrings.FirebaseReference.RESTAURANT_NAME).getValue().toString().equalsIgnoreCase(intentRestaurant)) {

                listOfCoworkers.add(item.child(RepoStrings.FirebaseReference.USER_FIRST_NAME).getValue().toString()
                        + " "
                        + item.child(RepoStrings.FirebaseReference.USER_LAST_NAME).getValue().toString());

            }
        }

        return listOfCoworkers;
    }

    /** Method that returns a groupKey using a datasnapshot
     * */
    public static String getGroupKeyFromDataSnapshot (DataSnapshot dataSnapshot, String group) {
        Log.d(TAG, "getGroupKeyFromDataSnapshot: called!");

        for (DataSnapshot item :
                dataSnapshot.getChildren()) {

            if (item.child(RepoStrings.FirebaseReference.GROUP_NAME).getValue().toString().equalsIgnoreCase(group)) {
                return item.getKey();

            }
        }

        return null;
    }







}