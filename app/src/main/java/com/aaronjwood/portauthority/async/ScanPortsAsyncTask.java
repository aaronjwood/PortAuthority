package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;
import android.util.Log;

import com.aaronjwood.portauthority.callable.ScanPortsCallable;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScanPortsAsyncTask extends AsyncTask<Object, Void, ArrayList<Integer>> {

    private static final String TAG = "ScanPortsAsyncTask";
    private HostAsyncResponse delegate;

    public ScanPortsAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ArrayList<Integer> doInBackground(Object... params) {
        final int NUM_THREADS = 500;
        String ip = (String) params[0];
        int startPort = (int) params[1];
        int stopPort = (int) params[2];

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int chunk = (int) Math.ceil((double) stopPort / NUM_THREADS);
        int previousStart = startPort;
        int previousStop = chunk;

        ArrayList<Future<ArrayList<Integer>>> futures = new ArrayList<>();

        for(int i = 0; i < NUM_THREADS; i++) {
            if(previousStop >= stopPort) {
                previousStop = stopPort;
                futures.add(executor.submit(new ScanPortsCallable(ip, previousStart, previousStop, delegate)));
                break;
            }
            futures.add(executor.submit(new ScanPortsCallable(ip, previousStart, previousStop, delegate)));
            previousStart = previousStop + 1;
            previousStop = previousStop + chunk;
        }

        executor.shutdown();

        try {
            ArrayList<Integer> ports = new ArrayList<>();
            for(Future<ArrayList<Integer>> future : futures) {
                ports.addAll(future.get());
            }
            return ports;
        }
        catch(InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(ExecutionException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<Integer> result) {
        delegate.processFinish(result);
    }
}
