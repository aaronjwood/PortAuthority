package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

public class Discovery {

    private static final String TAG = "Discovery";

    public void scanHosts(String ip, MainAsyncResponse delegate) {
        new ScanHostsAsyncTask(delegate).execute(ip);
    }
}
