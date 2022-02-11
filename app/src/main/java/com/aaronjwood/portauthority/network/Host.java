package com.aaronjwood.portauthority.network;

import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

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
    private final String ip;
    private final byte[] address;
    private final String mac;
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
     */
    public void setHostname(String hostname) {
        if (hostname != null && hostname.isEmpty() && hostname.endsWith(".local")) {
            hostname = hostname.substring(0, hostname.length() - 6);
        }
        this.hostname = hostname;

    }

    private void setVendor(Database db) {
        vendor = findMacVendor(mac, db);

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
        new WolAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mac, ip);
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
        new ScanPortsAsyncTask(delegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ip, startPort, stopPort, timeout);
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
    public static String findMacVendor(String mac, Database db) throws SQLiteException {
        String prefix = mac.substring(0, 8);
        String vendor = db.selectVendor(prefix);
        if (vendor != null) {
            return vendor;
        }

        String notInDb = "Vendor not in database";
        char identifier = mac.charAt(1);
        if ("26ae".indexOf(identifier) != -1) {
            return notInDb + " (private address)";
        }

        if ("13579bdf".indexOf(identifier) != -1) {
            return notInDb + " (multicast address)";
        }

        return notInDb;
    }
}
