package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

public class Discovery {

    /**
     * Starts the host scanning
     *
     * @param ip       IP address
     * @param delegate Delegate to be called when the host scan is finished
     */
    public static void scanHosts(String ip, MainAsyncResponse delegate) {
        new ScanHostsAsyncTask(delegate).execute(ip);
    }
}
