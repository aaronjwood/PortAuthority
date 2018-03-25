package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public abstract class Network {

    public class NoConnectivityManagerException extends Exception {
    }

    protected Context context;

    Network(Context context) {
        this.context = context;
    }

    /**
     * Gets the Android network information in the context of the current activity
     *
     * @return Network information
     */
    NetworkInfo getNetworkInfo(int type) throws NoConnectivityManagerException {
        return getConnectivityManager().getNetworkInfo(type);
    }

    /**
     * Gets the Android connectivity manager in the context of the current activity
     *
     * @return Connectivity manager
     */
    private ConnectivityManager getConnectivityManager() throws NoConnectivityManagerException {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            throw new NoConnectivityManagerException();
        }

        return manager;
    }

}
