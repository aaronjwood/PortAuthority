package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.AsyncResponse;
import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;

public class Discovery {

    private static final String TAG = "Discovery";

    public void scanHosts(String ip, AsyncResponse delegate) {
        new ScanHostsAsyncTask(delegate).execute(ip);
    }
}
