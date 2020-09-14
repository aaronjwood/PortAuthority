package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;

import com.aaronjwood.portauthority.async.WanIpAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.net.InetAddress;
import java.util.List;

public abstract class Network {

    public static class NoConnectivityManagerException extends Exception {
    }

    public static class SubnetNotFoundException extends Exception {
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
    protected ConnectivityManager getConnectivityManager() throws NoConnectivityManagerException {
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

    /**
     * Gets the interface's MAC address.
     *
     * @return MAC address.
     * @throws Exception
     */
    abstract String getMacAddress() throws Exception;

    /**
     * Gets the interface's subnet.
     *
     * @return
     * @throws SubnetNotFoundException
     * @throws NoConnectivityManagerException
     */
    public int getSubnet() throws SubnetNotFoundException, NoConnectivityManagerException {
        ConnectivityManager cm = getConnectivityManager();
        LinkProperties linkProperties = cm.getLinkProperties(cm.getActiveNetwork());
        if (linkProperties == null) {
            throw new SubnetNotFoundException();
        }

        List<LinkAddress> addresses = linkProperties.getLinkAddresses();
        for (LinkAddress address : addresses) {
            InetAddress addr = address.getAddress();
            if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                return address.getPrefixLength();
            }
        }

        throw new SubnetNotFoundException();
    }

    /**
     * Returns the number of hosts in the subnet.
     *
     * @return Number of hosts.
     */
    public int getNumberOfSubnetHosts(int subnet) {
        double bitsLeft = 32.0d - (double) subnet;
        double hosts = Math.pow(2.0d, bitsLeft) - 2.0d;

        return (int) hosts;
    }

    /**
     * Gets the device's external (WAN) IP address
     *
     * @param delegate Called when the external IP address has been fetched
     */
    public void getWanIp(MainAsyncResponse delegate) {
        new WanIpAsyncTask(delegate).execute();
    }

}
