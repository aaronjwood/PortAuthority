package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Wired extends Network {

    public Wired(Context context) {
        super(context);
    }

    /**
     * Determines if the device is connected to a wired network.
     *
     * @return
     * @throws NoConnectivityManagerException
     */
    public boolean isConnected() throws NoConnectivityManagerException {
        NetworkInfo info = getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        return info != null && info.isConnectedOrConnecting();
    }

}
