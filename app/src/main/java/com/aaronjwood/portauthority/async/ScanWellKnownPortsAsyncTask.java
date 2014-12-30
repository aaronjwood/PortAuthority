package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;
import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.callable.ScanPortsCallable;

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
        String ip = params[0];

        ExecutorService executor = Executors.newFixedThreadPool(8);
        Future<ArrayList<Integer>> ports1 = executor.submit(new ScanPortsCallable(ip, 1, 128, delegate));
        Future<ArrayList<Integer>> ports2 = executor.submit(new ScanPortsCallable(ip, 129, 257, delegate));
        Future<ArrayList<Integer>> ports3 = executor.submit(new ScanPortsCallable(ip, 258, 386, delegate));
        Future<ArrayList<Integer>> ports4 = executor.submit(new ScanPortsCallable(ip, 387, 515, delegate));
        Future<ArrayList<Integer>> ports5 = executor.submit(new ScanPortsCallable(ip, 516, 644, delegate));
        Future<ArrayList<Integer>> ports6 = executor.submit(new ScanPortsCallable(ip, 645, 773, delegate));
        Future<ArrayList<Integer>> ports7 = executor.submit(new ScanPortsCallable(ip, 774, 902, delegate));
        Future<ArrayList<Integer>> ports8 = executor.submit(new ScanPortsCallable(ip, 903, 1024, delegate));
        executor.shutdown();

        try {
            ArrayList<Integer> ports = new ArrayList<>();
            ports.addAll(ports1.get());
            ports.addAll(ports2.get());
            ports.addAll(ports3.get());
            ports.addAll(ports4.get());
            ports.addAll(ports5.get());
            ports.addAll(ports6.get());
            ports.addAll(ports7.get());
            ports.addAll(ports8.get());
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
