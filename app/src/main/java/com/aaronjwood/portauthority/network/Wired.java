package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Wired extends Network {

    public Wired(Context context) {
        super(context);
    }

    /**
     * Checks if ethernet is connected.
     *
     * @return True if connected or in the process of connecting, false otherwise.
     * @throws NoConnectivityManagerException
     */
    @Override
    public boolean isConnected() throws NoConnectivityManagerException {
        NetworkInfo info = getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        return info != null && info.isConnectedOrConnecting();
    }

}
