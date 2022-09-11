package com.aaronjwood.portauthority.async;

import android.content.Context;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.MDNSResolver;
import com.aaronjwood.portauthority.network.NetBIOSResolver;
import com.aaronjwood.portauthority.network.Resolver;
import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.runnable.ScanHostsRunnable;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanHostsAsyncTask extends AsyncTask<Integer, Void, Void> {
    private final WeakReference<MainAsyncResponse> delegate;
    private final Database db;
    private static final String NEIGHBOR_INCOMPLETE = "INCOMPLETE";
    private static final String NEIGHBOR_FAILED = "FAILED";

    static {
        System.loadLibrary("ipneigh");
    }

    public native int nativeIPNeigh(int fd);

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when host discovery has finished
     */
    public ScanHostsAsyncTask(MainAsyncResponse delegate, Database db) {
        this.delegate = new WeakReference<>(delegate);
        this.db = db;
    }

    /**
     * Scans for active hosts on the network
     *
     * @param params IP address
     */
    @Override
    protected Void doInBackground(Integer... params) {
        int ipv4 = params[0];
        int cidr = params[1];
        int timeout = params[2];
        MainAsyncResponse activity = delegate.get();
        ExecutorService executor = Executors.newCachedThreadPool();

        double hostBits = 32.0d - cidr; // How many bits do we have for the hosts.
        int netmask = (0xffffffff >> (32 - cidr)) << (32 - cidr); // How many bits for the netmask.
        int numberOfHosts = (int) Math.pow(2.0d, hostBits) - 2; // 2 ^ hostbits = number of hosts in integer.
        int firstAddr = (ipv4 & netmask) + 1; // AND the bits we care about, then first addr.

        int SCAN_THREADS = (int) hostBits;
        int chunk = (int) Math.ceil((double) numberOfHosts / SCAN_THREADS); // Chunk hosts by number of threads.
        int previousStart = firstAddr;
        int previousStop = firstAddr + (chunk - 2); // Ignore network + first addr

        for (int i = 0; i < SCAN_THREADS; i++) {
            executor.execute(new ScanHostsRunnable(previousStart, previousStop, timeout, delegate));
            previousStart = previousStop + 1;
            previousStop = previousStart + (chunk - 1);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
            executor.shutdownNow();
        } catch (InterruptedException e) {
            activity.processFinish(e);
            return null;
        }

        publishProgress();
        return null;
    }

    /**
     * Scans the ARP table and updates the list with hosts on the network
     * Resolves both DNS and NetBIOS
     * Don't update the UI in onPostExecute since we want to do multiple UI updates here
     * onPostExecute seems to perform all UI updates at once which would hinder what we're doing here
     * TODO: complexity has gone down but method is still too big.
     *
     * @param params
     */
    @Override
    protected final void onProgressUpdate(Void... params) {
        final MainAsyncResponse activity = delegate.get();
        ExecutorService executor = Executors.newCachedThreadPool();
        final AtomicInteger numHosts = new AtomicInteger(0);
        List<Pair<String, String>> pairs = new ArrayList<>();
        Context ctx = (Context) activity;

        ParcelFileDescriptor[] pipe;
        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            reportError(activity, e);
            cleanup(executor, activity, null);
            return;
        }

        ParcelFileDescriptor readSidePfd = pipe[0];
        ParcelFileDescriptor writeSidePfd = pipe[1];
        ParcelFileDescriptor.AutoCloseInputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readSidePfd);
        int fd_write = writeSidePfd.detachFd();
        int returnCode = nativeIPNeigh(fd_write);
        if (returnCode != 0) {
            reportError(activity, new Exception(ctx.getResources().getString(R.string.errAccessArp)));
            cleanup(executor, activity, null);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        while (true) {
            String line;
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                reportError(activity, e);
                cleanup(executor, activity, reader);
                return;
            }

            String[] neighborLine = line.split("\\s+");

            // We don't have a validated ARP entry for this case.
            if (neighborLine.length <= 4) {
                continue;
            }

            String ip = neighborLine[0];
            InetAddress addr;
            try {
                addr = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                reportError(activity, e);
                cleanup(executor, activity, reader);
                return;
            }

            if (addr.isLinkLocalAddress() || addr.isLoopbackAddress()) {
                continue;
            }

            String macAddress = neighborLine[4];
            String state = neighborLine[neighborLine.length - 1];

            // Determine if the ARP entry is valid.
            // https://github.com/sivasankariit/iproute2/blob/master/ip/ipneigh.c
            if (!NEIGHBOR_FAILED.equals(state) && !NEIGHBOR_INCOMPLETE.equals(state)) {
                pairs.add(new Pair<>(ip, macAddress));
            }
        }

        numHosts.addAndGet(pairs.size());
        for (Pair<String, String> pair : pairs) {
            String ip = pair.first;
            String macAddress = pair.second;
            executor.execute(() -> {
                Host host;
                try {
                    host = new Host(ip, macAddress, db);
                } catch (UnknownHostException e) {
                    reportError(activity, e);
                    cleanup(executor, activity, reader);
                    return;
                }

                MainAsyncResponse activity1 = delegate.get();
                InetAddress add;
                try {
                    add = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    numHosts.decrementAndGet();
                    reportError(activity1, e);
                    cleanup(executor, activity, reader);
                    return;
                }

                String hostname = add.getCanonicalHostName();
                host.setHostname(hostname);

                if (activity1 != null) {
                    activity1.processFinish(host, numHosts);
                }

                // BUG: Some devices don't respond to mDNS if NetBIOS is queried first. Why?
                // So let's query mDNS first, to keep in mind for eventual UPnP implementation.
                try {
                    if (resolve(ip, host, activity1, numHosts, new MDNSResolver(UserPreference.getLanSocketTimeout(ctx)))) {
                        return;
                    }

                    resolve(ip, host, activity1, numHosts, new NetBIOSResolver(UserPreference.getLanSocketTimeout(ctx)));
                } catch (Exception ignored) {
                }
            });
        }

        cleanup(executor, activity, reader);
    }

    private void reportError(MainAsyncResponse activity, Exception e) {
        if (activity != null) {
            activity.processFinish(e);
        }
    }

    private void cleanup(@NonNull ExecutorService executor, MainAsyncResponse activity, Reader reader) {
        executor.shutdown();
        if (activity != null) {
            activity.processFinish(true);
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean resolve(String ip, Host host, MainAsyncResponse activity, AtomicInteger numHosts, Resolver resolver) {
        InetAddress add;
        try {
            add = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            resolver.close();
            return false;
        }

        String[] name;
        try {
            name = resolver.resolve(add);
        } catch (IOException e) {
            resolver.close();
            return false;
        }

        resolver.close();
        if (name != null && name[0] != null && !name[0].isEmpty()) {
            host.setHostname(name[0]);
            if (activity != null) {
                // Call with null to refresh
                activity.processFinish(null, numHosts);
            }

            return true;
        }

        return false;
    }
}
