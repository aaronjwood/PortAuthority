package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.GetHostnameAsyncTask;
import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

public class Host {

    private static final String TAG = "Host";

    public void scanPorts(String ip, int startPort, int stopPort, HostAsyncResponse delegate) {
        new ScanPortsAsyncTask(delegate).execute(ip, startPort, stopPort);
    }

    public void getHostname(String ip, HostAsyncResponse delegate) {
        new GetHostnameAsyncTask(delegate).execute(ip);
    }

}
