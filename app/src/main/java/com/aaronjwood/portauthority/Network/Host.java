package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.GetHostnameAsyncTask;
import com.aaronjwood.portauthority.async.ScanWellKnownPortsAsyncTask;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

public class Host {

    private static final String TAG = "Host";

    public void scanWellKnownPorts(String ip, HostAsyncResponse delegate) {
        new ScanWellKnownPortsAsyncTask(delegate).execute(ip);
    }

    public void getHostname(String ip, HostAsyncResponse delegate) {
        new GetHostnameAsyncTask(delegate).execute(ip);
    }

}
