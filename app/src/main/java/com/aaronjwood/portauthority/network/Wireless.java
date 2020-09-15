package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class Wireless extends Network {

    public static class NoWifiManagerException extends Exception {
    }

    /**
     * Constructor to set the activity for context
     *
     * @param context The activity to use for context
     */
    public Wireless(Context context) {
        super(context);
    }

    /**
     * Gets the signal strength of the wireless network that the device is connected to
     *
     * @return Signal strength
     */
    public int getSignalStrength() throws NoWifiManagerException {
        return getWifiInfo().getRssi();
    }

    /**
     * Gets the BSSID of the wireless network that the device is connected to
     *
     * @return BSSID
     */
    public String getBSSID() throws NoWifiManagerException {
        return getWifiInfo().getBSSID();
    }

    /**
     * Gets the SSID of the wireless network that the device is connected to
     *
     * @return SSID
     */
    public String getSSID() throws NoWifiManagerException {
        String ssid = getWifiInfo().getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    /**
     * Gets the current link speed of the wireless network that the device is connected to
     *
     * @return Wireless link speed
     */
    public int getLinkSpeed() throws NoWifiManagerException {
        return getWifiInfo().getLinkSpeed();
    }

    /**
     * Determines if WiFi is enabled on the device or not
     *
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() throws NoWifiManagerException {
        return getWifiManager().isWifiEnabled();
    }

    /**
     * Gets the Android WiFi manager in the context of the current activity
     *
     * @return WifiManager
     */
    private WifiManager getWifiManager() throws NoWifiManagerException {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            throw new NoWifiManagerException();
        }

        return manager;
    }

    /**
     * Gets the Android WiFi information in the context of the current activity
     *
     * @return WiFi information
     */
    private WifiInfo getWifiInfo() throws NoWifiManagerException {
        return getWifiManager().getConnectionInfo();
    }

}
