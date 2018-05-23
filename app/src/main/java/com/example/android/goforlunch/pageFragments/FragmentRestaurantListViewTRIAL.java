package com.example.android.goforlunch.pageFragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.activities.MainActivity;
import com.example.android.goforlunch.data.AppDatabase;
import com.example.android.goforlunch.data.AppExecutors;
import com.example.android.goforlunch.data.RestaurantEntry;
import com.example.android.goforlunch.data.sqlite.AndroidDatabaseManager;
import com.example.android.goforlunch.helpermethods.Anim;
import com.example.android.goforlunch.recyclerviewadapter.RVAdapterList;
import com.example.android.goforlunch.strings.StringValues;
import com.example.android.goforlunch.data.viewmodel.MainViewModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Diego Fajardo on 27/04/2018.
 */

public class FragmentRestaurantListViewTRIAL extends Fragment {

    // TODO: 24/05/2018 Add the ViewModel here

    private static final String TAG = "PageFragmentRestaurantL";

    //Widgets
    private TextView mErrorMessageDisplay;
    private ProgressBar mProgressBar;
    private Toolbar toolbar;
    private RelativeLayout toolbar2;
    private ActionBar actionBar;

    //List of elements
    private List<RestaurantEntry> listOfRestaurants;
    private List<RestaurantEntry> listOfRestaurantsByType;

    //Widgets
    private AutoCompleteTextView mSearchText;

    //RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Database
    private AppDatabase mDb;
    private MainViewModel mainViewModel;

    /******************************
     * STATIC METHOD FOR **********
     * INSTANTIATING THE FRAGMENT *
     *****************************/

    public static FragmentRestaurantListViewTRIAL newInstance() {
        FragmentRestaurantListViewTRIAL fragment = new FragmentRestaurantListViewTRIAL();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView: Map");

        View view = inflater.inflate(R.layout.fragment_restaurant_list_view, container, false);

        /** Activates the toolbar menu for the fragment
         * */
        setHasOptionsMenu(true);

        mDb = AppDatabase.getInstance(getActivity());

        mainViewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        mainViewModel.getRestaurants().observe(this, new Observer<List<RestaurantEntry>>() {
            @Override
            public void onChanged(@Nullable List<RestaurantEntry> restaurantEntries) {
                Log.d(TAG, "onChanged: Retrieving data from LiveData inside ViewModel");

                if (restaurantEntries != null) {
                    Log.d(TAG, "onChanged: restaurantEntries.size() = " + restaurantEntries.size());
                    listOfRestaurants = restaurantEntries;

                } else {
                    Log.d(TAG, "onChanged: restaurantEntries is NULL");
                }
            }
        });

        toolbar = (Toolbar) view.findViewById(R.id.list_main_toolbar_id);
        toolbar2 = (RelativeLayout) view.findViewById(R.id.list_toolbar_search_id);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list_recycler_view_id);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RVAdapterList(getContext(), listOfRestaurants);
        mRecyclerView.setAdapter(mAdapter);

        mSearchText = (AutoCompleteTextView) view.findViewById(R.id.map_autocomplete_id);

        if (getActivity() != null) {
            ArrayAdapter<String> autocompleteAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_1, //This layout has to be a textview
                    StringValues.RESTAURANT_TYPES
            );

            mSearchText.setAdapter(autocompleteAdapter);
            mSearchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                    Log.d(TAG, "onItemClick: CALLED!");

                    if (!Arrays.asList(StringValues.RESTAURANT_TYPES).contains(adapterView.getItemAtPosition(i).toString())) {
                        listOfRestaurantsByType = listOfRestaurants;
                        mAdapter = new RVAdapterList(getContext(), listOfRestaurantsByType);
                        mRecyclerView.setAdapter(mAdapter);
                    } else if (Arrays.asList(StringValues.RESTAURANT_TYPES).contains(adapterView.getItemAtPosition(i).toString())) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                listOfRestaurantsByType = mDb.restaurantDao().getAllRestaurantsByType(adapterView.getItemAtPosition(i).toString());
                                mAdapter = new RVAdapterList(getContext(), listOfRestaurantsByType);
                                mRecyclerView.setAdapter(mAdapter);
                            }
                        });
                    }
                }
            });

        }

        if (((AppCompatActivity) getActivity()) != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        if (((AppCompatActivity) getActivity()) != null) {
            actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        Anim.crossFadeShortAnimation(mRecyclerView);

        return view;

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        getActivity().getMenuInflater().inflate(R.menu.list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home: {
                Log.d(TAG, "onOptionsItemSelected: home clicked");
                if (((MainActivity)getActivity()) != null) {
                    ((MainActivity)getActivity()).getMDrawerLayout().openDrawer(GravityCompat.START);
                }
                return true;
            }

            case R.id.list_search_button_id: {

                Log.d(TAG, "onOptionsItemSelected: clicked!");
                // TODO: 21/05/2018 Remove

                startActivity(new Intent(getActivity(), AndroidDatabaseManager.class));
//                AppExecutors.getInstance().diskIO().execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        mDb.restaurantDao().updateRestaurantDistance("ChIJiQCNwNqNcUgRIXPF8SO44lo", "0.1m");
//                    }
//                });

                // TODO: 21/05/2018 Uncomment
                //Log.d(TAG, "onOptionsItemSelected: search button clicked");
                //toolbar.setVisibility(View.GONE);
                //Anim.crossFadeShortAnimation(toolbar2);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

}
