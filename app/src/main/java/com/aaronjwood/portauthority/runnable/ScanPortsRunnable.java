package com.aaronjwood.portauthority.runnable;

import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class ScanPortsRunnable implements Runnable {

    private static final String TAG = "ScanPortsRunnable";

    private String ip;
    private int startPort;
    private int stopPort;
    private HostAsyncResponse delegate;

    /**
     * Constructor to set the necessary data to perform a port scan
     *
     * @param ip        IP address
     * @param startPort Port to start scanning at
     * @param stopPort  Port to stop scanning at
     * @param delegate  Called when this chunk of ports has finished scanning
     */
    public ScanPortsRunnable(String ip, int startPort, int stopPort, HostAsyncResponse delegate) {
        this.ip = ip;
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.delegate = delegate;
    }

    /**
     * Starts the port scan
     */
    @Override
    public void run() {
        for(int i = this.startPort; i <= this.stopPort; i++) {
            try {
                this.delegate.processFinish(0);
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.connect(new InetSocketAddress(this.ip, i), 3500);
                socket.close();
                this.delegate.processFinish(i);
            }
            catch(SocketException e) {
                Log.e(TAG, e.getMessage());
            }
            catch(IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
