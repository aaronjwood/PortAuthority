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

public class ScanWellKnownPortsAsyncTask extends AsyncTask<String, Void, ArrayList<Integer>> {

    private static final String TAG = "ScanWellKnownPortsAsyncTask";
    private HostAsyncResponse delegate;

    public ScanWellKnownPortsAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ArrayList<Integer> doInBackground(String... params) {
        final int NUM_THREADS = 64;
        String ip = params[0];

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int chunk = 1024 / NUM_THREADS;
        int previousStart = 1;
        int previousStop = chunk;

        ArrayList<Future<ArrayList<Integer>>> futures = new ArrayList<>();

        for(int i = 0; i < NUM_THREADS; i++) {
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
