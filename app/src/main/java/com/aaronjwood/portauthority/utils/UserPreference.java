package com.aaronjwood.portauthority.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility Class for getting certain user preferences
 */
public class UserPreference {

    private static final String KEY_HOST_ADDRESS = "HOST_ADDRESS_STRING";
    private static final String KEY_DOMAIN_NAME = "DOMAIN_NAME_STRING";
    private static final String KEY_DNS_RECORD = "DNS_RECORD_STRING";
    private static final String KEY_PORT_RANGE_START = "KEY_PORT_RANGE_MIN_INT";
    private static final String KEY_PORT_RANGE_STOP = "KEY_PORT_RANGE_HIGH_INT";
    private static final String PORT_SCAN_THREADS = "portScanThreads";
    private static final String EXTERNAL_IP = "externalIp";
    private static final String LAN_SOCKET_TIMEOUT = "lanTimeout";
    private static final String WAN_SOCKET_TIMEOUT = "wanTimeout";
    private static final String HOST_SOCKET_TIMEOUT = "hostTimeout";
    private static final String COARSE_LOCATION_PERM_DIAG = "COARSE_LOCATION";
    private static final String FINE_LOCATION_PERM_DIAG = "FINE_LOCATION";

    private static final String DEFAULT_WAN_SOCKET_TIMEOUT = "8000";
    private static final String DEFAULT_LAN_SOCKET_TIMEOUT = "4000";
    private static final String DEFAULT_HOST_SOCKET_TIMEOUT = "150";
    private static final String DEFAULT_PORT_SCAN_THREADS = "500";


    /**
     * Saves the state of the coarse location permission dialog.
     */
    public static void saveCoarseLocationPermDiag(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(COARSE_LOCATION_PERM_DIAG, true).apply();
    }

    /**
     * Saves the state of the fine location permission dialog.
     */
    public static void saveFineLocationPermDiag(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(FINE_LOCATION_PERM_DIAG, true).apply();
    }

    /**
     * Saves the state of the coarse location permission dialog.
     */
    public static boolean getCoarseLocationPermDiag(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(COARSE_LOCATION_PERM_DIAG, false);
    }

    /**
     * Saves the state of the fine location permission dialog.
     */
    public static boolean getFineLocationPermDiag(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(FINE_LOCATION_PERM_DIAG, false);
    }

    /**
     * Gets the last used host address or an empty string if there isn't one.
     */
    @NonNull
    public static String getLastUsedHostAddress(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(KEY_HOST_ADDRESS, "");
    }

    /**
     * Saves the last entered domain name for DNS lookups
     *
     * @param context
     * @param domainName
     */
    public static void saveLastUsedDomainName(@NonNull Context context, @Nullable String domainName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (domainName == null) {
            preferences.edit().remove(KEY_DOMAIN_NAME).apply();
        } else {
            preferences.edit().putString(KEY_DOMAIN_NAME, domainName).apply();
        }
    }

    /**
     * Gets the last entered domain name for DNS lookups
     *
     * @param context
     */
    public static String getLastUsedDomainName(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(KEY_DOMAIN_NAME, "");
    }

    /**
     * Saves the last used DNS record for DNS lookups
     *
     * @param context
     * @param index
     */
    public static void saveLastUsedDnsRecord(@NonNull Context context, int index) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(KEY_DNS_RECORD, index).apply();
    }

    /**
     * Gets the last used DNS record for DNS lookups
     *
     * @param context
     * @return
     */
    public static int getLastUsedDnsRecord(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(KEY_DNS_RECORD, 0);
    }

    /**
     * Saves the last used port range start value.
     */
    public static void savePortRangeStart(@NonNull Context context,
                                          @IntRange(from = Constants.MIN_PORT_VALUE, to = Constants.MAX_PORT_VALUE) int port) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(KEY_PORT_RANGE_START, port).apply();
    }

    /**
     * Gets the last used port range start value.
     */
    public static int getPortRangeStart(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(KEY_PORT_RANGE_START, Constants.MIN_PORT_VALUE);
    }

    /**
     * Saves the last used port range stop value.
     */
    public static void savePortRangeHigh(@NonNull Context context,
                                         @IntRange(from = Constants.MIN_PORT_VALUE, to = Constants.MAX_PORT_VALUE) int port) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(KEY_PORT_RANGE_STOP, port).apply();
    }

    /**
     * Gets the last used port range stop value.
     */
    public static int getPortRangeHigh(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(KEY_PORT_RANGE_STOP, Constants.MAX_PORT_VALUE);
    }

    /**
     * Gets the number of threads used for scanning ports
     *
     * @param context
     * @return Number of threads for scanning ports
     */
    public static int getPortScanThreads(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String threads = preferences.getString(PORT_SCAN_THREADS, DEFAULT_PORT_SCAN_THREADS);
        if (threads.isEmpty() || threads.equals("0")) {
            return Integer.parseInt(DEFAULT_PORT_SCAN_THREADS);
        }

        return Integer.parseInt(threads);
    }

    /**
     * Gets the setting that controls whether or not the external IP should be fetched
     *
     * @param context
     * @return True if the external IP should be fetched, false if it shouldn't
     */
    public static boolean getFetchExternalIp(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(EXTERNAL_IP, true);
    }

    /**
     * Gets the socket timeout that's used when scanning ports on the LAN
     *
     * @param context
     * @return Socket timeout
     */
    public static int getLanSocketTimeout(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeout = preferences.getString(LAN_SOCKET_TIMEOUT, DEFAULT_LAN_SOCKET_TIMEOUT);
        if (timeout.isEmpty()) {
            return Integer.parseInt(DEFAULT_LAN_SOCKET_TIMEOUT);
        }

        return Integer.parseInt(timeout);
    }

    /**
     * Gets the socket timeout that's used when scanning ports on the WAN
     *
     * @param context
     * @return Socket timeout
     */
    public static int getWanSocketTimeout(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeout = preferences.getString(WAN_SOCKET_TIMEOUT, DEFAULT_WAN_SOCKET_TIMEOUT);
        if (timeout.isEmpty()) {
            return Integer.parseInt(DEFAULT_WAN_SOCKET_TIMEOUT);
        }

        return Integer.parseInt(timeout);
    }

    /**
     * Gets the socket timeout that's used when scanning for hosts on the LAN
     *
     * @param context
     * @return Socket timeout
     */
    public static int getHostSocketTimeout(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeout = preferences.getString(HOST_SOCKET_TIMEOUT, DEFAULT_HOST_SOCKET_TIMEOUT);
        if (timeout.isEmpty()) {
            return Integer.parseInt(DEFAULT_HOST_SOCKET_TIMEOUT);
        }

        return Integer.parseInt(timeout);
    }
}
