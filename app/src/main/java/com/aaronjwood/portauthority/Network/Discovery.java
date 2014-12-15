package com.aaronjwood.portauthority.Network;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Discovery extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "Discovery";

    private Activity activity;
    private String ip;

    public Discovery(Activity activity, String ip) {
        this.activity = activity;
        this.ip = ip;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String parts[] = this.ip.split("\\.");

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

        return null;
    }

    protected void onPostExecute(Void result) {
        Toast.makeText(this.activity.getApplicationContext(), "Host discovery finished!", Toast.LENGTH_SHORT).show();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            ListView hostList = (ListView) this.activity.findViewById(R.id.hostList);
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) hostList.getAdapter();

            while((line = reader.readLine()) != null) {
                String[] l = line.split("\\s+");

                String flag = l[2];
                String macAddress = l[3];

                if(!flag.equals("0x0") && !macAddress.equals("00:00:00:00:00:00")) {
                    adapter.add(l[0]);
                }
            }

            adapter.sort(new Comparator<String>() {

                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            });

            adapter.notifyDataSetChanged();
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static class ScanHostsRunnable implements Runnable {

        private static final String TAG = "ScanHostsRunnable";

        private String[] ipParts;
        private int start;
        private int stop;

        public ScanHostsRunnable(String[] ipParts, int start, int stop) {
            this.ipParts = ipParts;
            this.start = start;
            this.stop = stop;
        }

        @Override
        public void run() {
            for(int i = this.start; i <= this.stop; i++) {
                String newIp = this.ipParts[0] + "." + this.ipParts[1] + "." + this.ipParts[2] + "." + i;
                InetAddress address;
                try {
                    address = InetAddress.getByName(newIp);
                    address.isReachable(75);
                }
                catch(UnknownHostException e) {
                    Log.e(this.TAG, e.getMessage());
                }
                catch(IOException e) {
                    Log.e(this.TAG, e.getMessage());
                }
            }

        }
    }
}
