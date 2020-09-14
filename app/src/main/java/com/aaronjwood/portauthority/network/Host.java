package com.aaronjwood.portauthority.network;

import android.database.sqlite.SQLiteException;

import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.async.WolAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host implements Serializable {

    private String hostname;
    private String ip;
    private byte[] address;
    private String mac;
    private String vendor;

    public Host(String ip, String mac, Database db) throws IOException {
        this(ip, mac);
        setVendor(db);
    }

    /**
     * Constructs a host with a known IP and MAC.
     *
     * @param ip
     * @param mac
     */
    public Host(String ip, String mac) throws UnknownHostException {
        this.ip = ip;
        this.address = InetAddress.getByName(ip).getAddress();
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

    private Host setVendor(Database db) throws IOException {
        vendor = findMacVendor(mac, db);

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
     * Returns this host's address in byte representation.
     *
     * @return
     */
    public byte[] getAddress() {
        return this.address;
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
     * Searches for the MAC vendor based on the provided MAc address.
     *
     * @param mac
     * @param db
     * @return
     * @throws IOException
     * @throws SQLiteException
     */
    public static String findMacVendor(String mac, Database db) throws IOException, SQLiteException {
        String prefix = mac.substring(0, 8);
        return db.selectVendor(prefix);
    }

}
