package com.example.android.goforlunch.activities.auth;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.android.goforlunch.R;

/**
 * Created by Diego Fajardo on 07/05/2018.
 */

public class AuthResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = AuthResetPasswordActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_reset_password);
    }
}