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

    public Wireless(Activity activity) {
        this.activity = activity;
    }

    public String getMacAddress() {
        return this.getWifiInfo().getMacAddress();
    }

    public boolean isHidden() {
        return this.getWifiInfo().getHiddenSSID();
    }

    public int getSignalStrength() {
        return this.getWifiInfo().getRssi();
    }

    public String getBSSID() {
        return this.getWifiInfo().getBSSID();
    }

    public String getSSID() {
        return this.getWifiInfo().getSSID();
    }

    public String getInternalIpAddress() {
        int ip = this.getWifiInfo().getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ip & 0xff),
                (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void getExternalIpAddress(MainAsyncResponse delegate) {
        new GetExternalIpAsyncTask(delegate).execute();
    }

    public int getLinkSpeed() {
        return this.getWifiInfo().getLinkSpeed();
    }

    public boolean isConnected() {
        return this.getNetworkInfo().isConnected();
    }

    public WifiManager getWifiManager() {
        return (WifiManager) this.activity.getSystemService(Context.WIFI_SERVICE);
    }

    private WifiInfo getWifiInfo() {
        return this.getWifiManager().getConnectionInfo();
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private NetworkInfo getNetworkInfo() {
        return this.getConnectivityManager().getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

}
