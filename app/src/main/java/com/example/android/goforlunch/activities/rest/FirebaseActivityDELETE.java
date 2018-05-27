package com.example.android.goforlunch.activities.rest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.activities.auth.RVAdapterRestaurantDELETE;
import com.example.android.goforlunch.data.AppDatabase;
import com.example.android.goforlunch.pojo.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Created by Diego Fajardo on 07/05/2018.
 */

// TODO: 20/05/2018 See UDACITY, Android Development, Android Architecture Components, 23 and forward
// TODO: 20/05/2018 to see how to work with search queries by id
public class FirebaseActivityDELETE extends AppCompatActivity {

    private static final String TAG = "FirebaseActivityDELETE";

    private Button button1;
    private Button button2;
    private Button button3;

    private HashMap<String,String> userData;

    private FirebaseDatabase fDb;
    private DatabaseReference dbRefUsers;
    private DatabaseReference dbRefGroups;

    private ChildEventListener mChildEventListener;

    //Recycler View
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RVAdapterRestaurantDELETE mAdapter;

    //Database
    private AppDatabase mDb;

    //Variables for objects
    private String placeId;
    private String name;
    private String type;
    private String address;
    private String openUntil;
    private String distance;
    private String rating;
    private String imageUrl;

    private static int counter = 0;

    private List<User> listOfUsers;

    //List of Fake Data
    private List<Name> listOfNames;
    private List<String> listOfEmails;
    private List<String> listOfGroups;
    private List<String> listOfRestaurants;
    private Map<String, Object> mapEmailGroup;

    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        random = new Random();

        listOfUsers = new ArrayList<>();
        listOfEmails = new ArrayList<>();
        listOfGroups = new ArrayList<>();
        listOfRestaurants = new ArrayList<>();

        listOfGroups.add("Amazon");
        listOfGroups.add("Google");
        listOfGroups.add("Apple");
        listOfGroups.add("Samsung");

        listOfRestaurants.add("Burger King");
        listOfRestaurants.add("McDonalds");
        listOfRestaurants.add("KFC");
        listOfRestaurants.add("Tony Romas");

        final NameGenerator generator = new NameGenerator();

        listOfNames = generator.generateNames(45);

        for (int i = 0; i < listOfNames.size(); i++) {

            listOfEmails.add(
                    listOfNames.get(i).getFirstName().toLowerCase()
                            + "_"
                            + listOfNames.get(i).getLastName().toLowerCase()
                            + "@gmail.com");

        }

        fDb = FirebaseDatabase.getInstance();
        dbRefUsers = fDb.getReference("users/");
        dbRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mapEmailGroup = new HashMap<>();

                if (dataSnapshot.getChildren() != null) {

                    Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                    Log.d(TAG, "onDataChange: KEY = " + dataSnapshot.getKey());

                    String userEmail;
                    String userGroup;

                    Map<String, Object> map;

                    for (DataSnapshot item :
                            dataSnapshot.getChildren()) {
                        Log.d(TAG, "onDataChange: DATASNAPSHOT = " + item.toString());

                        userEmail = Objects.requireNonNull(item.child("email").getValue()).toString();
                        userGroup = Objects.requireNonNull(item.child("group").getValue()).toString();

                        mapEmailGroup.put(userEmail, userGroup);

                    }

                    Log.d(TAG, "onDataChange: " + mapEmailGroup);

                    dbRefGroups = fDb.getReference("groups/");
                    for (int i = 0; i < listOfGroups.size(); i++) {

                        map = new HashMap<>();
                        map.put("group" + i, "");

                        dbRefGroups.updateChildren(map);
                    }

                    for (int i = 0; i < listOfGroups.size(); i++) {

                        dbRefGroups = fDb.getReference("groups/group" + i);
                        map = new HashMap<>();

                        map.put("name", listOfGroups.get(i));
                        map.put("members","");

                        dbRefGroups.updateChildren(map);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.getCode());

            }
        });

        button1 = findViewById(R.id.firebase_button_add_restaurants_id);
        button2 = findViewById(R.id.firebase_button_add_group_id);
        button3 = findViewById(R.id.firebase_button_add_place_id);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: CALLED!");

                Map<String,Object> map;

                for (int i = 0; i < 45; i++) {

                    map = new HashMap<>();
                    map.put("userid" + i, listOfEmails.get(i));

                    dbRefUsers = fDb.getReference("users/userid" + i);
                    map = new HashMap<>();
                    map.put("firstname",listOfNames.get(i).getFirstName());
                    map.put("lastname",listOfNames.get(i).getLastName());
                    map.put("email",listOfEmails.get(i));
                    map.put("group",listOfGroups.get(random.nextInt(4)));
                    map.put("placeId","");
                    map.put("restaurant","");
                    map.put("restaurantType","");
                    map.put("rating","");
                    map.put("phone","");
                    map.put("image_url","");

                    dbRefUsers.updateChildren(map);
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: CALLED!");

            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: CALLED!");

                Map<String,Object> map = new HashMap<>();
                String modEmail;

                for (int i = 0; i < listOfGroups.size(); i++) {

                    dbRefGroups = fDb.getReference("groups/group" + i + "/" + "members");

                    for (Map.Entry<String,Object> entry:
                            mapEmailGroup.entrySet()) {

                        map = new HashMap<>();

                        Log.d(TAG, "onClick: entry.getValue() = " + entry.getValue());
                        Log.d(TAG, "onClick: listOfGroups.get(i) = " + listOfGroups.get(i));

                        if (entry.getValue().toString().equals(listOfGroups.get(i))){

                            modEmail = entry.getKey();
                            if (modEmail.contains(".")) {
                                modEmail = modEmail.replace(".",",");
                            }

                            map.put(modEmail, true);
                            dbRefGroups.updateChildren(map);
                        }
                    }
                }
            }
        });

        /** com.google.firebase.database.DatabaseException: Serializing Arrays is not supported, please use Lists instead */

        /**

         "requests" : {
         "-KSVYZwUQPfyosiyRVdr" : {
         "interests" : { "x": true },
         "live" : true,
         "uIds" : {
         "user1": true,
         "user2": true
         }
         },
         "-KSl1L60g0tW5voyv0VU" : {
         "interests" : { "y": true },
         "live" : true,
         "uIds" : {
         "user2": true
         }
         }
         }

         */
    }
}