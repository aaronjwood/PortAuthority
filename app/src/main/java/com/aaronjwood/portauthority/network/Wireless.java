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
    private WifiManager wifi;
    private WifiInfo wifiInfo;
    private ConnectivityManager connection;
    private NetworkInfo networkInfo;

    public Wireless(Activity activity) {
        this.activity = activity;
        this.wifi = (WifiManager) this.activity.getSystemService(Context.WIFI_SERVICE);
        this.wifiInfo = this.wifi.getConnectionInfo();
        this.connection = (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.networkInfo = this.connection.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public String getMacAddress() {
        return this.wifiInfo.getMacAddress();
    }

    public boolean isHidden() {
        return this.wifiInfo.getHiddenSSID();
    }

    public int getSignalStrength() {
        this.wifiInfo = this.wifi.getConnectionInfo();
        return this.wifiInfo.getRssi();
    }

    public String getBSSID() {
        return this.wifiInfo.getBSSID();
    }

    public String getSSID() {
        return this.wifiInfo.getSSID();
    }

    public String getInternalIpAddress() {
        int ip = this.wifiInfo.getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ip & 0xff),
                (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void getExternalIpAddress(MainAsyncResponse delegate) {
        new GetExternalIpAsyncTask(delegate).execute();
    }

    public int getLinkSpeed() {
        return this.wifiInfo.getLinkSpeed();
    }

    public boolean isConnected() {
        return this.networkInfo.isConnected();
    }

}
