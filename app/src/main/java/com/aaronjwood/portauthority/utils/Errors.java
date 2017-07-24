package com.aaronjwood.portauthority.utils;

import android.content.Context;
import android.widget.Toast;

public class Errors {

    public static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
