package com.example.android.goforlunch.activities.auth;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.activities.rest.MainActivity;
import com.example.android.goforlunch.broadcastreceivers.InternetConnectionReceiver;
import com.example.android.goforlunch.helpermethods.ToastHelper;
import com.example.android.goforlunch.helpermethods.Utils;
import com.example.android.goforlunch.helpermethods.UtilsFirebase;
import com.example.android.goforlunch.repository.RepoStrings;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
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
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.observers.DisposableObserver;

/**
 * Created by Diego Fajardo on 24/07/2018.
 */
public class AuthChooseLoginActivity extends AppCompatActivity implements Observer {

    private static final String TAG = AuthChooseLoginActivity.class.getSimpleName();

    //Google Sign In Request Code
    private static int RC_GOOGLE_SIGN_IN = 101;

    //Widgets

    @BindView(R.id.choose_progressbar_id)
    ProgressBar progressBar;

    @BindView(R.id.choose_sign_in_password_button_id)
    Button buttonPassword;

    @BindView(R.id.choose_textView_register)
    TextView tvRegister;

    @BindView(R.id.choose_google_sign_in_button)
    SignInButton buttonGoogle;

    @BindView(R.id.choose_facebook_sign_in_button)
    LoginButton buttonFacebook;

    @BindView(R.id.progressBar_content)
    LinearLayout progressBarContent;

    @BindView(R.id.main_layout_id)
    LinearLayout mainContent;

    private GoogleSignInClient mGoogleSignInClient;

    //For facebook login
    private CallbackManager mCallbackManager;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseDatabase fireDb;
    private DatabaseReference dbRefUsers;

    private SharedPreferences sharedPref;

    //InternetConnectionReceiver variables
    private InternetConnectionReceiver receiver;
    private IntentFilter intentFilter;
    private Snackbar snackbar;

    private boolean internetAvailable;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called!");

        /* We delete all info from shared preferences
        * */
        sharedPref = PreferenceManager.getDefaultSharedPreferences(AuthChooseLoginActivity.this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear().apply();

        /* We establish the entry points
        to get the user information
        * */
        fireDb = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        // TODO: 29/06/2018 Delete signOut()
        auth.signOut();
        LoginManager.getInstance().logOut();

        /* internetAvailable is false till the update() callback changes it
        * */
        internetAvailable = false;

        /* We set the content view
        * */
        setContentView(R.layout.activity_auth_choose_login);
        ButterKnife.bind(this);

        /* We check if the user is logged in in a background thread.
        * */
        checkIfUserIsLoggedInBackgroundThread();

        /* We set the listeners
        * */
        buttonPassword.setOnClickListener(buttonPasswordOnClickListener);

        tvRegister.setOnClickListener(tvRegisterOnClickListener);

        /* Configure Google Sign In
         *  */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        buttonGoogle.setStyle(SignInButton.SIZE_WIDE,SignInButton.COLOR_DARK);
        buttonGoogle.setOnClickListener(buttonGoogleOnClickListener);

        mCallbackManager = CallbackManager.Factory.create();
        buttonFacebook.setReadPermissions("email", "public_profile", "user_friends");
        buttonFacebook.registerCallback(mCallbackManager, facebookCallbackLoginResult);

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: called!");

        receiver = new InternetConnectionReceiver();
        intentFilter = new IntentFilter(RepoStrings.CONNECTIVITY_CHANGE_STATUS);
        Utils.connectReceiver(AuthChooseLoginActivity.this, receiver, intentFilter, this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: called!");

        if (receiver != null) {
            Utils.disconnectReceiver(
                    AuthChooseLoginActivity.this,
                    receiver,
                    AuthChooseLoginActivity.this);
        }

        receiver = null;
        intentFilter = null;
        snackbar = null;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called!");

        if (receiver != null) {
            Utils.disconnectReceiver(
                    AuthChooseLoginActivity.this,
                    receiver,
                    AuthChooseLoginActivity.this);
        }

        receiver = null;
        intentFilter = null;
        snackbar = null;

        buttonPassword.setOnClickListener(null);
        tvRegister.setOnClickListener(null);
        buttonGoogle.setOnClickListener(null);

    }

    /** Callback: listening to broadcast receiver
     * */
    @Override
    public void update(Observable o, Object internetAvailableUpdate) {
        Log.d(TAG, "update: called!");

        if ((int) internetAvailableUpdate == 0) {
            Log.d(TAG, "update: Internet Not Available");

            internetAvailable = false;

            if (snackbar == null) {
                snackbar = Utils.createSnackbar(
                        AuthChooseLoginActivity.this,
                        mainContent,
                        getResources().getString(R.string.noInternetFeaturesNotWork));

            } else {
                snackbar.show();
            }

        } else {
            Log.d(TAG, "update: Internet available");

            internetAvailable = true;

            if (snackbar != null) {
                snackbar.dismiss();
            }

            /* We get the user info. If the user is already registered and we have his/her name
            and last name,
            we launch MainActivity
            * */
            getUserAndLaunchSpecificActivity();

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called!");


        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Log.d(TAG, "onActivityResult: google process..!");
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, updateItem UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }

        } else {
            // Result returned from launching the Intent from Facebook Login
            Log.d(TAG, "onActivityResult: facebook process...!");
            mCallbackManager.onActivityResult(requestCode,resultCode,data);

        }
    }

    /*******************************
     * LISTENERS *******************
     ******************************/

    private View.OnClickListener buttonPasswordOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: Password Button clicked!");

            startActivity(new Intent(AuthChooseLoginActivity.this, AuthSignInEmailPasswordActivity.class));

        }
    };

    private View.OnClickListener tvRegisterOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: Register textView clicked!");

            Intent intent = new Intent(AuthChooseLoginActivity.this, AuthEnterNameActivity.class);

            //We include a FLAG intent extra (boolean) to notify the next activity we launched the intent from this Activity
            intent.putExtra(RepoStrings.SentIntent.FLAG, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }
    };


    private View.OnClickListener buttonGoogleOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: Google Button clicked!");

            if (internetAvailable) {
                googleSignIn();

            } else {
                ToastHelper.toastNoInternet(AuthChooseLoginActivity.this);

            }

        }
    };

    private FacebookCallback<LoginResult> facebookCallbackLoginResult = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            Log.d(TAG, "onSuccess: called!");

            if (internetAvailable) {
                handleFacebookAccessToken(loginResult.getAccessToken());

            } else {
                ToastHelper.toastNoInternet(AuthChooseLoginActivity.this);
            }

        }

        @Override
        public void onCancel() {
            Log.d(TAG, "onCancel: called!");

        }

        @Override
        public void onError(FacebookException error) {
            Log.e(TAG, "onError: " + error.toString() );

        }
    };


    /** Method that checks if the user is currently
     * logged in in a background thread
     * */
    private void checkIfUserIsLoggedInBackgroundThread() {
        Log.d(TAG, "checkIfUserIsLoggedInBackgroundThread: called!");

        /* We use this method instead of internetAvailable because we are still
        * in onCreate() and internetAvailable would be false in both cases (with
        * and without internet available)
        * */
        Utils.checkInternetInBackgroundThread(new DisposableObserver<Boolean>() {
            @Override
            public void onNext(Boolean internetAvailableBackgroundThread) {
                Log.d(TAG, "onNext: called!");

                if (!internetAvailableBackgroundThread) {
                    ToastHelper.toastNoInternetFeaturesNotWorking(AuthChooseLoginActivity.this);
                    Utils.showMainContent(progressBarContent, mainContent);

                } else {
                    /* Internet is available
                    * */
                    getUserAndLaunchSpecificActivity();
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

    /** Method that gets the user and launches an specific activity if needed
     * */
    private void getUserAndLaunchSpecificActivity () {
        Log.d(TAG, "getUserAndLaunchSpecificActivity: called!");

        user = auth.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "onCreate: user is null");
            //We delete Shared preferences info
            Utils.deleteSharedPreferencesInfo(sharedPref);
            // TODO: 13/06/2018 Remove all info in SharedPref

            /* If the user is null, we won't launch a new activity,
             so we show the main layout
            * */
            Utils.showMainContent(progressBarContent, mainContent);


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
    }

    /** Method for google sign in
     * */
    public void googleSignIn() {
        Log.d(TAG, "googleSignIn: called!");

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    /********************
     * SIGN IN **********
     * with google ******
     * and facebook *****
     * *****************/

    /** Method that handles google sign in
     * */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle: called!");
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        if (internetAvailable) {
            /* We hide the main screen while the process runs
             * */
            Utils.hideMainContent(progressBarContent, mainContent);

            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success
                                Log.d(TAG, "GOOGLE signInWithCredential: success");
                                user = auth.getCurrentUser();

                                if (user != null) {
                                    checkIfUserExistsInDatabase(user);

                                }

                            } else {
                                /* Something went wrong during sign in*/
                                Log.d(TAG, "GOOGLE signInWithCredential: failure");
                                ToastHelper.toastShort(AuthChooseLoginActivity.this, getResources().getString(R.string.somethingWentWrong));
                                Utils.showMainContent(progressBarContent, mainContent);

                            }
                        }
                    });

        } else {
            /* There is no internet
            * */
            ToastHelper.toastNoInternet(AuthChooseLoginActivity.this);
        }
    }

    /** Method that handles facebook sign in
     * */
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken: called!");

        if (internetAvailable) {

            Utils.hideMainContent(progressBarContent, mainContent);

            AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, updateItem UI with the signed-in user's information
                                Log.d(TAG, "FACEBOOK signInWithCredential: success");
                                user = auth.getCurrentUser();

                                if (user != null) {
                                    checkIfUserExistsInDatabase(user);

                                }

                            } else {
                                /* If sign in fails, display a message to the user
                                 * hide progress bar and enable onClick on views
                                 * */
                                Log.w(TAG, "FACEBOOK signInWithCredential: failure", task.getException());
                                ToastHelper.toastShort(AuthChooseLoginActivity.this, getResources().getString(R.string.somethingWentWrong));
                                Utils.showMainContent(progressBarContent, mainContent);

                            }

                            // ...
                        }
                    });

        } else {
            ToastHelper.toastNoInternet(AuthChooseLoginActivity.this);
        }

    }

    /** Method that checks if a user exists in the database.
     * If the user does, MainActivity is launched. If the user doesn't,
     * the user is created in the database and afterwards MainActivity is launched
     * */
    private void checkIfUserExistsInDatabase (final FirebaseUser user) {
        Log.d(TAG, "checkIfUserExistsInDatabase: called!");

        final DatabaseReference fireDbUsersRef = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
        fireDbUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "GOOGLE onDataChange: " + dataSnapshot.toString());

                boolean userExists = false;

                for (DataSnapshot item :
                        dataSnapshot.getChildren()) {

                    if (Objects.requireNonNull(user.getEmail()).equalsIgnoreCase(
                            Objects.requireNonNull(item.child(RepoStrings.FirebaseReference.USER_EMAIL).getValue()).toString())) {

                        /* The user already exists, so we launch MainActivity
                         * */
                        userExists = true;

                        // TODO: 24/07/2018 Check!
                        fireDbUsersRef.removeEventListener(this);

                        Intent intent = new Intent(AuthChooseLoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;

                    }
                }

                if (internetAvailable) {

                    /* The for loop ends. If the user does not exist in the database, we insert the user
                     * */
                    if (!userExists) {
                        Log.d(TAG, "GOOGLE onDataChange: user does not exist");

                        dbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
                        String userKey = dbRefUsers.push().getKey();

                        String[] names = Utils.getFirstNameAndLastName(user.getDisplayName());

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
                                0,
                                "");

                        Intent intent = new Intent(AuthChooseLoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                    }

                } else {
                    /* Internet is not available
                    * */
                    ToastHelper.toastSomethingWentWrong(AuthChooseLoginActivity.this);
                    Utils.showMainContent(progressBarContent, mainContent);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "GOOGLE onCancelled: " + databaseError.getCode());

            }
        });

    }

}
