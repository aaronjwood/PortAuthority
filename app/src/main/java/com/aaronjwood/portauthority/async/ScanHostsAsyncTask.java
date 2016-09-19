package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanHostsRunnable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jcifs.netbios.NbtAddress;

public class ScanHostsAsyncTask extends AsyncTask<String, Void, Void> {
    private final WeakReference<MainAsyncResponse> delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when host discovery has finished
     */
    public ScanHostsAsyncTask(MainAsyncResponse delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    /**
     * Scans for active hosts on the network
     *
     * @param params IP address
     */
    @Override
    protected Void doInBackground(String... params) {
        String ip = params[0];
        String parts[] = ip.split("\\.");
        int hosts = Integer.decode(params[1]);

        ExecutorService executor = Executors.newCachedThreadPool();

        int SCAN_THREADS = 8;
        int chunk = (int) Math.ceil((double) hosts / SCAN_THREADS);
        int previousStart = 1;
        int previousStop = chunk;

        for (int i = 0; i < SCAN_THREADS; i++) {
            if (previousStop >= hosts) {
                previousStop = hosts;
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

    /**
     * Scans the ARP table and updates the list with hosts on the network
     * Resolves both DNS and NetBIOS
     * Don't update the UI in onPostExecute since we want to do multiple UI updates here
     * onPostExecute seems to perform all UI updates at once which would hinder what we're doing here
     * TODO: this method is gross, refactor it and break it up
     *
     * @param params
     */
    @Override
    protected final void onProgressUpdate(final Void... params) {
        BufferedReader reader = null;
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] arpLine = line.split("\\s+");

                final String ip = arpLine[0];
                String flag = arpLine[2];
                final String macAddress = arpLine[3];

                if (!"0x0".equals(flag) && !"00:00:00:00:00:00".equals(macAddress)) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> item = new HashMap<String, String>() {
                                @Override
                                public boolean equals(Object object) {
                                    if (this == object) {
                                        return true;
                                    }
                                    if (object == null) {
                                        return false;
                                    }
                                    if (!(object instanceof HashMap)) {
                                        return false;
                                    }

                                    @SuppressWarnings("unchecked")
                                    Map<String, String> entry = (Map<String, String>) object;
                                    return entry.get("Second Line").equals(this.get("Second Line"));
                                }
                            };

                            String secondLine = ip + " [" + macAddress + "]";
                            item.put("Second Line", secondLine);

                            try {
                                InetAddress add = InetAddress.getByName(ip);
                                String hostname = add.getCanonicalHostName();
                                item.put("First Line", hostname);

                                MainAsyncResponse activity = delegate.get();
                                if (activity != null) {
                                    activity.processFinish(item);
                                }
                            } catch (UnknownHostException ignored) {
                                return;
                            }

                            try {
                                NbtAddress[] netbios = NbtAddress.getAllByAddress(ip);
                                for (NbtAddress addr : netbios) {
                                    if (addr.getNameType() == 0x20) {
                                        item.put("First Line", addr.getHostName());

                                        MainAsyncResponse activity = delegate.get();
                                        if (activity != null) {
                                            activity.processFinish(item);
                                        }
                                        return;
                                    }
                                }
                            } catch (UnknownHostException ignored) {
                            }
                        }
                    });
                }
            }
            executor.shutdown();
        } catch (IOException ignored) {
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
