package com.aaronjwood.portauthority.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Utility Class for getting certain user preferences
 */
public class UserPreference {

    private static final String KEY_HOST_ADDRESS = "HOST_ADDRESS_STRING";

    /**
     * Saves the last used host address for later use.
     *
     * @param hostAddress the host address string or {@code null} to clear the saved value.
     */
    public static void saveLastUsedHostAddress(@NonNull Context context, @Nullable String hostAddress) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (hostAddress == null) {
            preferences.edit().remove(HOST_ADDRESS_KEY).apply();
        } else {
            preferences.edit().putString(HOST_ADDRESS_KEY, hostAddress).apply();
        }
    }

    /**
     * Gets the last used host address or an empty string if there isn't one.
     */
    @NonNull
    public static String getLastUsedHostAddress(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(HOST_ADDRESS_KEY, "");
    }
}
