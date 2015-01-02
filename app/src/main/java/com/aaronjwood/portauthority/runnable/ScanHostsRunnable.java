package com.aaronjwood.portauthority.runnable;

import android.util.Log;

import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ScanHostsRunnable implements Runnable {

    private static final String TAG = "ScanHostsRunnable";

    private String[] ipParts;
    private int start;
    private int stop;
    private MainAsyncResponse delegate;

    public ScanHostsRunnable(String[] ipParts, int start, int stop, MainAsyncResponse delegate) {
        this.ipParts = ipParts;
        this.start = start;
        this.stop = stop;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        for(int i = this.start; i <= this.stop; i++) {
            String newIp = this.ipParts[0] + "." + this.ipParts[1] + "." + this.ipParts[2] + "." + i;
            InetAddress address;
            try {

                address = InetAddress.getByName(newIp);
                address.isReachable(100);
            }
            catch(UnknownHostException e) {
                Log.e(this.TAG, e.getMessage());
            }
            catch(IOException e) {
                Log.e(this.TAG, e.getMessage());
            }
            finally {
                this.delegate.processFinish(1);
            }
        }
    }
}