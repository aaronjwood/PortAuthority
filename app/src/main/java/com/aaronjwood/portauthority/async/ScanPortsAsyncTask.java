package com.aaronjwood.portauthority.async;

import android.content.Context;
import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanPortsRunnable;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanPortsAsyncTask extends AsyncTask<Object, Void, Void> {
    private final WeakReference<HostAsyncResponse> delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when a port scan has finished
     */
    public ScanPortsAsyncTask(HostAsyncResponse delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    /**
     * Chunks the ports selected for scanning and starts the process
     * Chunked ports are scanned in parallel
     *
     * @param params IP address, start port, and stop port
     */
    @Override
    protected Void doInBackground(Object... params) {
        String ip = (String) params[0];
        int startPort = (int) params[1];
        int stopPort = (int) params[2];
        int timeout = (int) params[3];

        HostAsyncResponse activity = delegate.get();
        if (activity != null) {
            final int NUM_THREADS = UserPreference.getPortScanThreads((Context) activity);

            try {
                InetAddress address = InetAddress.getByName(ip);
                ip = address.getHostAddress();
            } catch (UnknownHostException e) {
                activity.processFinish(false);
                return null;
            }

            ExecutorService executor = Executors.newCachedThreadPool();

            int chunk = (int) Math.ceil((double) (stopPort - startPort) / NUM_THREADS);
            int previousStart = startPort;
            int previousStop = (startPort - 1) + chunk;

            for (int i = 0; i < NUM_THREADS; i++) {
                if (previousStop >= stopPort) {
                    previousStop = stopPort;
                    executor.execute(new ScanPortsRunnable(ip, previousStart, previousStop, timeout, delegate));
                    break;
                }
                executor.execute(new ScanPortsRunnable(ip, previousStart, previousStop, timeout, delegate));
                previousStart = previousStop + 1;
                previousStop = previousStop + chunk;
            }

            executor.shutdown();

            try {
                executor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException ignored) {
            }

            activity.processFinish(true);
        }

        return null;
    }
}
