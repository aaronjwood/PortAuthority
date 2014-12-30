package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.HostAsyncResponse;
import com.aaronjwood.portauthority.async.ScanWellKnownPortsAsyncTask;

public class Host {

    private static final String TAG = "Host";

    public void scanWellKnownPorts(String ip, HostAsyncResponse delegate) {
        new ScanWellKnownPortsAsyncTask(delegate).execute(ip);
    }

}
