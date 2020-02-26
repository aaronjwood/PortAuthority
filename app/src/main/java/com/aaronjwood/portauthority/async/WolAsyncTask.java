package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class WolAsyncTask extends AsyncTask<String, Void, Void> {

    /**
     * Sends the magic UDP packet to wake up a host
     *
     * @param params
     * @return Nothing
     */
    @Override
    protected Void doInBackground(String... params) {
        String mac = params[0];
        String ip = params[1];

        if (ip == null || mac == null || ip.isEmpty() || mac.isEmpty()) {
            return null;
        }

        byte[] macBytes = new byte[6];
        String[] macHex = mac.split("([:\\-])");
        if (macHex.length != 6) {
            return null;
        }

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
            address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 9);
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (IOException e) {
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return null;
    }
}
