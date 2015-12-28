package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanPortsRunnable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanPortsAsyncTask extends AsyncTask<Object, Void, Void> {
    private HostAsyncResponse delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when a port scan has finished
     */
    public ScanPortsAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    /**
     * Chunks the ports selected for scanning and starts the process
     * Chunked ports are scanned in parallel
     *
     * @param params IP address, start port, and stop port
     */
    @Override
    protected Void doInBackground(Object... params) {
        final int NUM_THREADS = 500;
        String ip = (String) params[0];
        int startPort = (int) params[1];
        int stopPort = (int) params[2];

        try {
            InetAddress address = InetAddress.getByName(ip);
            ip = address.getHostAddress();
        } catch (UnknownHostException e) {
            this.delegate.processFinish(false);
            return null;
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int chunk = (int) Math.ceil((double) (stopPort - startPort) / NUM_THREADS);
        int previousStart = startPort;
        int previousStop = (startPort - 1) + chunk;

        for (int i = 0; i < NUM_THREADS; i++) {
            if (previousStop >= stopPort) {
                previousStop = stopPort;
                executor.execute(new ScanPortsRunnable(ip, previousStart, previousStop, delegate));
                break;
            }
            executor.execute(new ScanPortsRunnable(ip, previousStart, previousStop, delegate));
            previousStart = previousStop + 1;
            previousStop = previousStop + chunk;
        }

        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }

        this.delegate.processFinish(true);

        return null;
    }
}
