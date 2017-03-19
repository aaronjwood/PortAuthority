package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

public class Discovery {

    /**
     * Starts the host scanning
     *
     * @param ip       IP address
     * @param cidr     Classless Inter-Domain Routing
     * @param timeout  Socket timeout
     * @param delegate Delegate to be called when the host scan is finished
     */
    public static void scanHosts(int ip, int cidr, int timeout, MainAsyncResponse delegate) {
        new ScanHostsAsyncTask(delegate).execute(ip, cidr, timeout);
    }
}
