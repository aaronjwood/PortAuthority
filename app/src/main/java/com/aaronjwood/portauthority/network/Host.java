package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.GetHostnameAsyncTask;
import com.aaronjwood.portauthority.async.GetMacInfoAsyncTask;
import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

public class Host {

    private static final String TAG = "Host";

    /**
     * Starts a port scan
     *
     * @param ip        IP address
     * @param startPort The port to start scanning at
     * @param stopPort  The port to stop scanning at
     * @param delegate  Delegate to be called when the port scan has finished
     */
    public void scanPorts(String ip, int startPort, int stopPort, HostAsyncResponse delegate) {
        new ScanPortsAsyncTask(delegate).execute(ip, startPort, stopPort);
    }

    /**
     * Fetches the hostname for a device on the network
     *
     * @param ip       IP address
     * @param delegate Delegate to be called when the hostname has been fetched
     */
    public void getHostname(String ip, HostAsyncResponse delegate) {
        new GetHostnameAsyncTask(delegate).execute(ip);
    }

    /**
     * Fetches additional MAC address information for the specified host
     *
     * @param mac      MAC address
     * @param delegate Delegate to be called when the MAC address information has been fetched
     */
    public void getMacInfo(String mac, HostAsyncResponse delegate) {
        new GetMacInfoAsyncTask(delegate).execute(mac);
    }

}
