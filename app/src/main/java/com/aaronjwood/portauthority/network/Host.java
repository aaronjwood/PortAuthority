package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.database.Cursor;

import com.aaronjwood.portauthority.async.ScanPortsAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Host implements Serializable {

    private String hostname;
    private String ip;
    private String mac;

    /**
     * Constructor to set necessary information without a known hostname
     *
     * @param ip  This host's IP address
     * @param mac This host's MAC address
     */
    public Host(String ip, String mac) {
        this(null, ip, mac);
    }

    /**
     * Constructor to set necessary information with a known hostname
     *
     * @param hostname This host's hostname
     * @param ip       This host's IP address
     * @param mac      This host's MAC address
     */
    public Host(String hostname, String ip, String mac) {
        this.hostname = hostname;
        this.ip = ip;
        this.mac = mac;
    }

    /**
     * Returns this host's hostname
     *
     * @return
     */
    public String getHostname() {
        return this.hostname;
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
     * Returns this host's IP address
     *
     * @return
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Returns this host's MAC address
     *
     * @return
     */
    public String getMac() {
        return this.mac;
    }

    public void wakeOnLan() throws IOException {
        byte[] macBytes = new byte[6];
        String[] macHex = this.mac.split("(:|-)");
        for (int i = 0; i < 6; i++) {
            macBytes[i] = (byte) Integer.parseInt(macHex[i], 16);
        }

        byte[] bytes = new byte[6 + 16 * macBytes.length];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }

        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        InetAddress address;
        DatagramSocket socket = null;
        try {
            address = InetAddress.getByName(this.ip);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 9);
            socket = new DatagramSocket();
            socket.send(packet);

        } catch (IOException e) {
            throw e;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
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
    public static String getMacVendor(String mac, Context context) {
        Database db = new Database(context);
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
