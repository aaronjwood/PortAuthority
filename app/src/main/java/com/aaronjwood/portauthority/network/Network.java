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
    private NetworkInfo getNetworkInfo() throws NoConnectivityManagerException {
        return getConnectivityManager().getActiveNetworkInfo();
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

    /**
     * Checks if there is an active connection.
     *
     * @return True if connected or in the process of connecting, false otherwise.
     * @throws NoConnectivityManagerException
     */
    public boolean isConnected() throws NoConnectivityManagerException {
        NetworkInfo info = getNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }

}
