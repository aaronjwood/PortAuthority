package com.aaronjwood.portauthority.callable;

import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ScanPortsCallable implements Callable<ArrayList<Integer>> {

    private static final String TAG = "ScanPortsCallable";

    private String ip;
    private int startPort;
    private int stopPort;
    private HostAsyncResponse delegate;

    public ScanPortsCallable(String ip, int startPort, int stopPort, HostAsyncResponse delegate) {
        this.ip = ip;
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.delegate = delegate;
    }

    @Override
    public ArrayList<Integer> call() {
        ArrayList<Integer> ports = new ArrayList<>();
        for(int i = this.startPort; i <= this.stopPort; i++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(this.ip, i), 1000);
                socket.close();
                ports.add(i);
            }
            catch(IOException e) {
                Log.e(TAG, e.getMessage());
            }
            finally {
                delegate.processFinish(1);
            }
        }
        return ports;
    }
}
