package com.aaronjwood.portauthority.network;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.aaronjwood.portauthority.async.GetExternalIpAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.util.Locale;

public class Wireless {

    private Activity activity;

    /**
     * Constructor to set the activity for context
     *
     * @param activity The activity to use for context
     */
    public Wireless(Activity activity) {
        this.activity = activity;
    }

    /**
     * Gets the MAC address of the device
     *
     * @return MAC address
     */
    public String getMacAddress() {
        return this.getWifiInfo().getMacAddress();
    }

    /**
     * Determines if the network that the device is connected to is a hidden network
     *
     * @return True if the network is hidden, false if it's not
     */
    public boolean isHidden() {
        return this.getWifiInfo().getHiddenSSID();
    }

    /**
     * Gets the signal strength of the wireless network that the device is connected to
     *
     * @return Signal strength
     */
    public int getSignalStrength() {
        return this.getWifiInfo().getRssi();
    }

    /**
     * Gets the BSSID of the wireless network that the device is connected to
     *
     * @return BSSID
     */
    public String getBSSID() {
        return this.getWifiInfo().getBSSID();
    }

    /**
     * Gets the SSID of the wireless network that the device is connected to
     *
     * @return SSID
     */
    public String getSSID() {
        return this.getWifiInfo().getSSID();
    }

    /**
     * Gets the device's internal (LAN) IP address
     *
     * @return LAN IP address
     */
    public String getInternalIpAddress() {
        int ip = this.getWifiInfo().getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ip & 0xff),
                (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    /**
     * Gets the device's external (WAN) IP address
     *
     * @param delegate Called when the external IP address has been fetched
     */
    public void getExternalIpAddress(MainAsyncResponse delegate) {
        new GetExternalIpAsyncTask(delegate).execute();
    }

    /**
     * Gets the current link speed of the wireless network that the device is connected to
     *
     * @return Wireless link speed
     */
    public int getLinkSpeed() {
        return this.getWifiInfo().getLinkSpeed();
    }

    /**
     * Determines if the device is connected to a network or not
     *
     * @return True if the device is connected, false if it isn't
     */
    public boolean isConnected() {
        return this.getNetworkInfo().isConnected();
    }

    /**
     * Gets the Android WiFi manager in the context of the current activity
     *
     * @return WifiManager
     */
    public WifiManager getWifiManager() {
        return (WifiManager) this.activity.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Gets the Android WiFi information in the context of the current activity
     *
     * @return WiFi information
     */
    private WifiInfo getWifiInfo() {
        return this.getWifiManager().getConnectionInfo();
    }

    /**
     * Gets the Android connectivity manager in the context of the current activity
     *
     * @return Connectivity manager
     */
    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Gets the Android network information in the context of the current activity
     *
     * @return Network information
     */
    private NetworkInfo getNetworkInfo() {
        return this.getConnectivityManager().getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

}
