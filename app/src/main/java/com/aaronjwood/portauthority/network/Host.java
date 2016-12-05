package com.aaronjwood.portauthority.network;

import android.app.Activity;
import android.database.Cursor;

import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

public class Host {

    private String hostname;
    private String ip;
    private String mac;

    public Host(String hostname, String ip, String mac) {
        this.hostname = hostname;
        this.ip = ip;
        this.mac = mac;
    }

    /**
     * Starts a port scan
     *
     * @param ip        IP address
     * @param startPort The port to start scanning at
     * @param stopPort  The port to stop scanning at
     * @param timeout   Socket timeout
     * @param delegate  Delegate to be called when the port scan has finished
     */
    public static void scanPorts(String ip, int startPort, int stopPort, int timeout, HostAsyncResponse delegate) {
        new ScanPortsAsyncTask(delegate).execute(ip, startPort, stopPort, timeout);
    }

    /**
     * Fetches the MAC vendor from the database
     *
     * @param mac      MAC address
     * @param activity The calling activity
     */
    public static String getMacVendor(String mac, Activity activity) {
        Database db = new Database(activity);
        Cursor cursor = db.queryDatabase("SELECT vendor FROM ouis WHERE mac LIKE ?", new String[]{mac});
        String vendor;

        try {
            if (cursor != null && cursor.moveToFirst()) {
                vendor = cursor.getString(cursor.getColumnIndex("vendor"));
            } else {
                vendor = "Vendor not in database";
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                db.close();
            }
        }

        return vendor;
    }

}
