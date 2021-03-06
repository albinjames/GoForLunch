package com.example.android.goforlunch.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.android.goforlunch.R;

/**
 * Created by Diego Fajardo on 27/04/2018.
 */

/** This class contains helper methods that
 * allow to create toasts easier and faster */
abstract public class ToastHelper {

    public static void toastShort(Context context, String string){
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    public static void toastLong(Context context, String string){
        Toast.makeText(context, string, Toast.LENGTH_LONG).show();
    }

    public static void toastNoInternet (Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
    }

    public static void toastNoInternetFeaturesNotWorking (Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.noInternetFeaturesNotWork), Toast.LENGTH_SHORT).show();
    }

    public static void toastSomethingWentWrong (Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.somethingWentWrong), Toast.LENGTH_SHORT).show();
    }

    public static void toastNotNecessaryPermissionsAvailable (Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.necessaryPermissionsNotGranted), Toast.LENGTH_SHORT).show();
    }



}
