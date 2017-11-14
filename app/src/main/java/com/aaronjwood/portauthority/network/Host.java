package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.async.WolAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.IOException;
import java.io.Serializable;

public class Host implements Serializable {

    private String hostname;
    private String ip;
    private String mac;
    private String vendor;

    /**
     * Constructs a host with a known IP and MAC, and additionally looks up the MAC vendor.
     *
     * @param ip
     * @param mac
     * @param context
     * @throws IOException
     */
    public Host(String ip, String mac, Context context) throws IOException {
        this(ip, mac);
        setVendor(context);
    }

    /**
     * Constructs a host with a known IP and MAC.
     *
     * @param ip
     * @param mac
     */
    public Host(String ip, String mac) {
        this.ip = ip;
        this.mac = mac;
    }

    /**
     * Returns this host's hostname
     *
     * @return
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets this host's hostname to the given value
     *
     * @param hostname Hostname for this host
     * @return
     */
    public Host setHostname(String hostname) {
        this.hostname = hostname;

        return this;
    }

    /**
     * Sets this host's MAC vendor.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private Host setVendor(Context context) throws IOException {
        String prefix = mac.replace(":", "").substring(0, 6);
        vendor = findMacVendor(prefix, context);

        return this;
    }

    /**
     * Gets this host's MAC vendor.
     *
     * @return
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Returns this host's IP address
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns this host's MAC address
     *
     * @return
     */
    public String getMac() {
        return mac;
    }

    public void wakeOnLan() {
        new WolAsyncTask().execute(mac, ip);
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
     * @param mac     MAC address
     * @param context Application context
     */
    public static String findMacVendor(String mac, Context context) throws IOException, SQLiteException {
        return new Database(context).selectVendor(mac);
    }

}
