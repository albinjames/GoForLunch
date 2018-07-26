package com.example.android.goforlunch;

import android.support.test.rule.ActivityTestRule;

import com.example.android.goforlunch.activities.auth.AuthChooseLoginActivity;
import com.example.android.goforlunch.constants.RepoStrings;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Created by Diego Fajardo on 25/07/2018.
 */
public class FirebaseTests {

    private static final String TAG = "FirebaseTests";

    /**
     * This RULE specifies that this activity is launched
     */
    //Always make this public
    @Rule
    public ActivityTestRule<AuthChooseLoginActivity> chooseLoginActivity =
            new ActivityTestRule<AuthChooseLoginActivity>(AuthChooseLoginActivity.class);

    private AuthChooseLoginActivity mActivity = null;

    private FirebaseDatabase fireDb = null;
    private DatabaseReference dbRef = null;

//    Instrumentation.ActivityMonitor searchArticlesMonitor =
//            getInstrumentation().addMonitor(
//                    SearchArticlesActivity.class.getName(),
//                    null,
//                    false);
//
//    /** Add the rest of activities
//     * */
//    Instrumentation.ActivityMonitor displayNotificationsActivityMonitor =
//            getInstrumentation().addMonitor(
//                    DisplayNotificationsActivity.class.getName(),
//                    null,
//                    false);
//


    @Before
    public void setUp() throws Exception {

        /** With this, we get the context! */
        mActivity = chooseLoginActivity.getActivity();

        fireDb = FirebaseDatabase.getInstance();

        assertThat(mActivity, notNullValue());
        assertThat(fireDb, notNullValue());

    }

//    @Test
//    public void modifySingleField () {
//
//        String userKey = "-LIFt4UPNY3CwP-wQLnj";
//        dbRef = fireDb.getReference(RepoStrings.FirebaseReference.USERS).child(userKey).child(RepoStrings.FirebaseReference.USER_NOTIFICATIONS);
//        dbRef.setValue(true);
//
//        //Sets "user_notifications" with the value true (it's a boolean, not "true" which will be a string)
//
//    }

    @Test
    public void addRestaurantToGroup () {

        String groupKey = "-LExsXwCe89xGdtaIK-I";
        String restaurant = "La Orza";

        Map<String, Object> map = new HashMap<>();
        map.put(restaurant, true);

        dbRef = fireDb.getReference(RepoStrings.FirebaseReference.GROUPS)
                .child(groupKey);
                //.child(RepoStrings.FirebaseReference.GROUP_RESTAURANTS_VISITED);
        dbRef.updateChildren(map);

    }


    @After
    public void tearDown() throws Exception {

        mActivity = null;
        fireDb = null;
        dbRef = null;

    }

}
