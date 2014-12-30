package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;
import android.util.Log;

import com.aaronjwood.portauthority.AsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanHostsRunnable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanHostsAsyncTask extends AsyncTask<Void, Void, ArrayList<Map<String, String>>> {

    private static final String TAG = "ScanHostsAsyncTask";
    public AsyncResponse delegate;
    private String ip;

    public ScanHostsAsyncTask(String ip) {
        this.ip = ip;
    }

    @Override
    protected ArrayList<Map<String, String>> doInBackground(Void... params) {
        String parts[] = ip.split("\\.");

        ExecutorService executor = Executors.newFixedThreadPool(8);
        executor.execute(new ScanHostsRunnable(parts, 1, 31));
        executor.execute(new ScanHostsRunnable(parts, 32, 63));
        executor.execute(new ScanHostsRunnable(parts, 64, 95));
        executor.execute(new ScanHostsRunnable(parts, 96, 127));
        executor.execute(new ScanHostsRunnable(parts, 128, 159));
        executor.execute(new ScanHostsRunnable(parts, 160, 191));
        executor.execute(new ScanHostsRunnable(parts, 192, 223));
        executor.execute(new ScanHostsRunnable(parts, 224, 255));
        executor.shutdown();

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

        return new ArrayList<>();
    }

    protected void onPostExecute(final ArrayList<Map<String, String>> result) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            while((line = reader.readLine()) != null) {
                String[] l = line.split("\\s+");

                final String ip = l[0];
                String flag = l[2];
                String macAddress = l[3];

                if(!flag.equals("0x0") && !macAddress.equals("00:00:00:00:00:00")) {
                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                InetAddress add = InetAddress.getByName(ip);
                                Map<String, String> entry = new HashMap<>();
                                entry.put("First Line", ip);
                                entry.put("Second Line", add.getHostName());
                                result.add(entry);
                            }
                            catch(UnknownHostException e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    });
                    thread.start();
                }
            }

            Collections.sort(result, new Comparator<Map<String, String>>() {

                @Override
                public int compare(Map<String, String> lhs, Map<String, String> rhs) {
                    return lhs.get("First Line").compareTo(rhs.get("First Line"));
                }
            });
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
