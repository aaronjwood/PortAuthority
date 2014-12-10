package com.aaronjwood.portauthority.Network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.util.Log;

public class Discovery {

    private String ip;
    private int port;

    public Discovery(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private int[] splitIp() {
        String[] parts = this.ip.split(".");
        int[] segments = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            segments[i] = Integer.parseInt(parts[i]);
        }

        return segments;
    }

    private void isReachable() {
        class Port implements Runnable {
            int port;

            Port(int port) {
                this.port = port;
            }

            @Override
            public void run() {
                try {
                    Socket s = new Socket();
                    s.connect(
                            new InetSocketAddress("192.168.1.149", this.port),
                            1000);
                    s.close();
                    Log.d("TEST", String.valueOf(this.port));
                }
                catch (IOException e) {

                }

            }

        }
        ;
        for (int i = 1; i < 200; i++) {
            Thread t = new Thread(new Port(i));
            t.start();
        }
    }

}
