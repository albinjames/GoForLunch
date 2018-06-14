package com.example.android.goforlunch.activities.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.activities.rest.MainActivity;
import com.example.android.goforlunch.helpermethods.ToastHelper;
import com.example.android.goforlunch.helpermethods.Utils;
import com.example.android.goforlunch.helpermethods.UtilsFirebase;
import com.example.android.goforlunch.repostrings.RepoStrings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

/**
 * Created by Diego Fajardo on 31/05/2018.
 */

// TODO: 02/06/2018  --------------------------- VERY IMPORTANT!!!!!! 
// TODO: 02/06/2018 Check that sign in works well when we close the app instead of login off
// TODO: 02/06/2018 Once, it allowed us to start with password account but the current user used gmail account
public class AuthChooseLoginActivity extends AppCompatActivity{

    private static final String TAG = "AuthChooseLoginActivity";

    //Google Sign In Request Code
    private static int RC_SIGN_IN = 101;

    private SignInButton buttonGoogle;
    private Button buttonFacebook;
    private Button buttonPassword;

    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseDatabase fireDb;
    private DatabaseReference dbRefUsers;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(AuthChooseLoginActivity.this);

        fireDb = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        auth.signOut();
        user = auth.getCurrentUser();

        Log.d(TAG, "onCreate: SHARED_PREFERENCES = " + sharedPref.getAll().toString());
        Log.d(TAG, "onCreate: user = " + user);

        if (user == null) {
            Log.d(TAG, "onCreate: user is null");
            //We delete Shared preferences info
            Utils.deleteSharedPreferencesInfo(sharedPref);
            // TODO: 13/06/2018 Remove all info in SharedPref


        } else {

            if (user.getDisplayName() != null){
                Log.d(TAG, "onCreate: user is not null");

                //go directly to MainActivity
                startActivity(new Intent(AuthChooseLoginActivity.this, MainActivity.class));
                finish();


            } else {

                //go to AuthEnterNameActivity
                startActivity(new Intent(AuthChooseLoginActivity.this, AuthEnterNameActivity.class));
                finish();

            }

        }

        /** We set the content view after checking if the user is logged in
         * */
        setContentView(R.layout.activity_auth_choose_login);

        buttonGoogle = findViewById(R.id.choose_google_sign_in_button);
        buttonFacebook = findViewById(R.id.choose_facebook_sign_in_button);
        buttonPassword = findViewById(R.id.choose_sign_in_password_button_id);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        buttonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Google Button clicked!");
                signIn();
            }
        });


        buttonFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Facebook Button clicked!");
                ToastHelper.toastShort(AuthChooseLoginActivity.this, "Facebook Button clicked");

            }
        });


        buttonPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Password Button clicked!");

                startActivity(new Intent(AuthChooseLoginActivity.this, AuthSignInPasswordActivity.class));
                finish();

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    public void signIn() {

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = auth.getCurrentUser();

                            if (user != null) {
                                Log.d(TAG, "onComplete: user != null");

                                if (user.getDisplayName() == null && user.getDisplayName().equalsIgnoreCase("")) {
                                    Log.d(TAG, "onComplete: user.getDisplayName() == null");
                                    /** If the user has not chosen yet a first name and last name we
                                     * launch AuthEnterNameActivity
                                     * */
                                    Intent intent = new Intent(AuthChooseLoginActivity.this, AuthEnterNameActivity.class);
                                    startActivity(intent);

                                } else {
                                    Log.d(TAG, "onComplete: user.getDisplayName() = " + user.getDisplayName());
                                     /** Two options. The user already exists
                                     * in Firebase Database or not.
                                     * */
                                    fireDb = FirebaseDatabase.getInstance();
                                    DatabaseReference fireDbUsersRef = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
                                    fireDbUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Log.d(TAG, "onDataChange: " + dataSnapshot.toString());

                                            boolean userExists = false;

                                            for (DataSnapshot item :
                                                    dataSnapshot.getChildren()) {

                                                if (Objects.requireNonNull(user.getEmail()).equalsIgnoreCase(
                                                        Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_EMAIL).getValue()).toString())) {
                                                    Log.d(TAG, "onDataChange: user already exists");

                                                    // TODO: 02/06/2018 Check this, might be able to be deleted
                                                    userExists = true;

                                                    Intent intent = new Intent(AuthChooseLoginActivity.this, MainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                    break;

                                                } else {

                                                }
                                            }

                                            if (!userExists) {
                                                /** If it is not in the foreach loop, then the user does not exist and we have to create him/her
                                                 * */
                                                Log.d(TAG, "onDataChange: user didn't exist");

                                                dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
                                                String userKey = dbRefUsers.push().getKey();

                                                String [] names = getFirstNameAndLastName();

                                                dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS + "/" + userKey);
                                                UtilsFirebase.updateUserInfoInFirebase(dbRefUsers,
                                                        names[0],
                                                        names[1],
                                                        user.getEmail().toLowerCase(),
                                                        "",
                                                        "",
                                                        "",
                                                        "");

                                                dbRefUsers = fireDb.getReference(
                                                        RepoStrings.FirebaseReference.USERS
                                                                + "/" + userKey
                                                                + "/" + RepoStrings.FirebaseReference.USER_RESTAURANT_INFO);
                                                UtilsFirebase.updateRestaurantsUserInfoInFirebase(dbRefUsers,
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "");

                                                Intent intent = new Intent(AuthChooseLoginActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();

                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.d(TAG, "onCancelled: " + databaseError.getCode());

                                        }
                                    });
                                }
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            ToastHelper.toastShort(AuthChooseLoginActivity.this, "Authentication Failed");
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }


    /** Method used to get the first name and last name of the user
     * */
    private String[] getFirstNameAndLastName () {

        String names = user.getDisplayName();

        if (names != null) {
            return names.split(" ");

        } else {
            return null;
        }
    }

}
