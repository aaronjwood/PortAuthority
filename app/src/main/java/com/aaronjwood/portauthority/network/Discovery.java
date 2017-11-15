package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

public class Discovery {

    public static void scanHosts(int ip, int cidr, int timeout, MainAsyncResponse delegate, Database db) {
        new ScanHostsAsyncTask(delegate, db).execute(ip, cidr, timeout);
    }
}
