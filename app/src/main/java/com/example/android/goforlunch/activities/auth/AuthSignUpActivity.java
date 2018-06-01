package com.example.android.goforlunch.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.android.goforlunch.R;
import com.example.android.goforlunch.helpermethods.ToastHelper;
import com.example.android.goforlunch.strings.RepoStrings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Diego Fajardo on 07/05/2018.
 */

public class AuthSignUpActivity extends AppCompatActivity {

    private static final String TAG = "AuthSignUpActivity";

    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;

    private Button buttonSignIn;
    private Button buttonSignUp;
    private Button buttonResetPassword;

    private ProgressBar progressBar;

    //List that contains the list of users in the database
    private List<String> listOfUsers;

    //Firebase
    private FirebaseAuth auth;
    private FirebaseDatabase fireDb;
    private DatabaseReference fireDbRefUsers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_signup);

        inputEmail = (TextInputEditText) findViewById(R.id.signup_email_id);
        inputPassword = (TextInputEditText) findViewById(R.id.signup_password_id);

        /** We get the info from the other screen AuthSignInPasswordActivity
         * */
        Intent intent = getIntent();
        if (intent.getStringExtra(RepoStrings.SentIntent.EMAIL) != null
                && intent.getStringExtra(RepoStrings.SentIntent.PASSWORD) != null) {

            inputEmail.setText(intent.getStringExtra(RepoStrings.SentIntent.EMAIL));
            inputPassword.setText(intent.getStringExtra(RepoStrings.SentIntent.PASSWORD));
        }

        buttonResetPassword = (Button) findViewById(R.id.signup_reset_button_id);
        buttonSignUp = (Button) findViewById(R.id.signup_register_button_id);
        buttonSignIn = (Button) findViewById(R.id.signup_registered_button_id);

        progressBar = (ProgressBar) findViewById(R.id.signup_progressbar);

        listOfUsers = new ArrayList<>();

        /** We get the list from users from the database to avoid creating two users
         * with the same email (which is the element we will use to identify the users)
         * */
        fireDb = FirebaseDatabase.getInstance();
        fireDbRefUsers = fireDb.getReference(RepoStrings.FirebaseReference.USERS);
        fireDbRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: " + dataSnapshot.toString());

                for (DataSnapshot item :
                        dataSnapshot.getChildren()) {

                    listOfUsers.add(Objects.requireNonNull(
                            item.child(RepoStrings.FirebaseReference.EMAIL).getValue()).toString().toLowerCase());

                }

                Log.d(TAG, "onDataChange: list.size() = " + listOfUsers.size());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });

        /** We get a reference to FirebaseAuth to create the user later
         * */
        auth = FirebaseAuth.getInstance();

        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastHelper.toastShort(AuthSignUpActivity.this, "Not implemented yet");
            }
        });

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(AuthSignUpActivity.this, AuthSignInPasswordActivity.class);
                intent.putExtra(RepoStrings.SentIntent.EMAIL,inputEmail.getText().toString().toLowerCase());
                intent.putExtra(RepoStrings.SentIntent.PASSWORD,inputPassword.getText().toString().toLowerCase());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = inputEmail.getText().toString().trim().toLowerCase();
                String password = inputPassword.getText().toString().trim();

                if (listOfUsers.size() > 0 &&
                        listOfUsers.contains(email)){
                    ToastHelper.toastShort(AuthSignUpActivity.this, "Sorry, that email is already in the database");

                } else if (TextUtils.isEmpty(email)) {
                    Log.d(TAG, "onClick: no email ");
                    ToastHelper.toastShort(AuthSignUpActivity.this, "Please, enter email");

                } else if (TextUtils.isEmpty(password)) {
                    Log.d(TAG, "onClick: no password");
                    ToastHelper.toastShort(AuthSignUpActivity.this, "Please, enter password");

                } else if (password.length() < 6) {
                    Log.d(TAG, "onClick: password too short, only " + password.length() + " characters");
                    ToastHelper.toastShort(AuthSignUpActivity.this, "Password is too short");

                } else {
                    Log.d(TAG, "onClick: not same email in database");

                    /** We proceed
                     * to next activity
                     * */
                    Intent intent = new Intent(AuthSignUpActivity.this, AuthEnterNameAndGroupActivity.class);
                    intent.putExtra(RepoStrings.SentIntent.EMAIL,email);
                    intent.putExtra(RepoStrings.SentIntent.PASSWORD,password);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();

                }
            }
        });
    }


}
