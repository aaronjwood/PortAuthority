package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanHostsRunnable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jcifs.netbios.NbtAddress;

public class ScanHostsAsyncTask extends AsyncTask<String, Void, Void> {
    private MainAsyncResponse delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when host discovery has finished
     */
    public ScanHostsAsyncTask(MainAsyncResponse delegate) {
        this.delegate = delegate;
    }

    /**
     * Scans for active hosts on the network
     *
     * @param params IP address
     * @return List to hold the active hosts
     */
    @Override
    protected Void doInBackground(String... params) {
        final int NUM_THREADS = 8;
        String ip = params[0];
        String parts[] = ip.split("\\.");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        int chunk = (int) Math.ceil((double) 255 / NUM_THREADS);
        int previousStart = 1;
        int previousStop = chunk;

        for (int i = 0; i < NUM_THREADS; i++) {
            if (previousStop >= 255) {
                previousStop = 255;
                executor.execute(new ScanHostsRunnable(parts, previousStart, previousStop, delegate));
                break;
            }
            executor.execute(new ScanHostsRunnable(parts, previousStart, previousStop, delegate));
            previousStart = previousStop + 1;
            previousStop = previousStop + chunk;
        }

        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        publishProgress();
        return null;
    }

    @Override
    protected final void onProgressUpdate(final Void... params) {
        try {
            final List<Map<String, String>> result = Collections.synchronizedList(new ArrayList<Map<String, String>>());
            ExecutorService executor = Executors.newCachedThreadPool();
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null) {
                String[] arpLine = line.split("\\s+");

                final String ip = arpLine[0];
                String flag = arpLine[2];
                final String macAddress = arpLine[3];

                if (!flag.equals("0x0") && !macAddress.equals("00:00:00:00:00:00")) {
                    final int finalI = i;
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                InetAddress add = InetAddress.getByName(ip);
                                String hostname = add.getCanonicalHostName();

                                Map<String, String> entry = new HashMap<>();
                                entry.put("First Line", hostname);
                                entry.put("Second Line", ip + " [" + macAddress + "]");
                                result.add(entry);
                                delegate.processFinish(result);
                            } catch (UnknownHostException ignored) {
                            }

                            try {
                                NbtAddress[] netbios = NbtAddress.getAllByAddress(ip);
                                final String netbiosName = netbios[0].getHostName();
                                result.set(finalI, new HashMap<String, String>() {{
                                    put("First Line", netbiosName);
                                    put("Second Line", ip + " [" + macAddress + "]");
                                }});
                                delegate.processFinish(result);
                            } catch (UnknownHostException ignored) {
                                return;
                            }
                        }
                    });
                    i++;
                }
            }

            reader.close();
            executor.shutdown();
        } catch (IOException ignored) {
        }
    }
}
