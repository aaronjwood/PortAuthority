package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;

import com.aaronjwood.portauthority.async.WanIpAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

public class Network {

    public static class NoConnectivityManagerException extends Exception {
    }

    public static class NetworkNotFoundException extends Exception {
    }

    public static class ConnectionInfo {
        public int subnet;
        public String ip;
        public String iface;
    }

    protected Context context;

    Network(Context context) {
        this.context = context;
    }

    /**
     * Gets the Android connectivity manager in the context of the current activity
     *
     * @return Connectivity manager
     */
    protected static ConnectivityManager getConnectivityManager(Context context) throws NoConnectivityManagerException {
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
    public static boolean isConnected(Context context) throws NoConnectivityManagerException {
        NetworkInfo info = getConnectivityManager(context).getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }

    public static ConnectionInfo getConnectionInfo(Context context) throws NoConnectivityManagerException, UnknownHostException, NetworkNotFoundException {
        ConnectivityManager cm = getConnectivityManager(context);
        android.net.Network activeNet = cm.getActiveNetwork();
        LinkProperties linkProperties = cm.getLinkProperties(activeNet);
        if (activeNet == null || linkProperties == null) {
            throw new NetworkNotFoundException();
        }

        List<LinkAddress> addresses = linkProperties.getLinkAddresses();
        for (LinkAddress address : addresses) {
            InetAddress addr = address.getAddress();
            if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                ConnectionInfo info = new ConnectionInfo();
                info.ip = activeNet.getByName(addr.getHostAddress()).getHostAddress();
                info.iface = linkProperties.getInterfaceName();
                info.subnet = address.getPrefixLength();
                return info;
            }
        }

        throw new NetworkNotFoundException();
    }

    public static String getMacAddress(Context context) throws NoConnectivityManagerException, SocketException, NetworkNotFoundException, UnknownHostException {
        NetworkInterface iface = NetworkInterface.getByName(getConnectionInfo(context).iface);
        byte[] mac = iface.getHardwareAddress();
        if (mac == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (byte aMac : mac) {
            buf.append(String.format("%02x:", aMac));
        }

        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }

        return buf.toString();
    }

    /**
     * Returns the number of hosts in the subnet.
     *
     * @return Number of hosts.
     */
    public static int getNumberOfSubnetHosts(int subnet) {
        double bitsLeft = 32.0d - (double) subnet;
        double hosts = Math.pow(2.0d, bitsLeft) - 2.0d;

        return (int) hosts;
    }

    /**
     * Gets the device's external (WAN) IP address
     *
     * @param delegate Called when the external IP address has been fetched
     */
    public static void getWanIp(MainAsyncResponse delegate) {
        new WanIpAsyncTask(delegate).execute();
    }

}
