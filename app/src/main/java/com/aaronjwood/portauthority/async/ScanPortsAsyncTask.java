package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;
import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanPortsRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanPortsAsyncTask extends AsyncTask<Object, Void, Void> {

    private static final String TAG = "ScanPortsAsyncTask";
    private HostAsyncResponse delegate;

    public ScanPortsAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(Object... params) {
        final int NUM_THREADS = 500;
        String ip = (String) params[0];
        int startPort = (int) params[1];
        int stopPort = (int) params[2];

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int chunk = (int) Math.ceil((double) (stopPort - startPort) / NUM_THREADS);
        int previousStart = startPort;
        int previousStop = (startPort - 1)+ chunk;

        for(int i = 0; i < NUM_THREADS; i++) {
            if(previousStop + 1 >= stopPort) {
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
        }
        catch(InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

        this.delegate.processFinish(true);

        return null;
    }
}
