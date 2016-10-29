package com.aaronjwood.portauthority.runnable;

import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ScanHostsRunnable implements Runnable {
    private int start;
    private int stop;
    private final WeakReference<MainAsyncResponse> delegate;

    /**
     * Constructor to set the necessary data to scan for hosts
     *
     * @param start    Host to start scanning at
     * @param stop     Host to stop scanning at
     * @param delegate Called when host discovery has finished
     */
    public ScanHostsRunnable(int start, int stop, WeakReference<MainAsyncResponse> delegate) {
        this.start = start;
        this.stop = stop;
        this.delegate = delegate;
    }

    /**
     * Starts the host discovery
     */
    @Override
    public void run() {
        for (int i = this.start; i <= this.stop; i++) {
            Socket socket = new Socket();
            try {
                socket.setTcpNoDelay(true);
                byte[] bytes = BigInteger.valueOf(i).toByteArray();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(bytes), 7), 150);
            } catch (IOException ignored) {
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }

                MainAsyncResponse activity = delegate.get();
                if (activity != null) {
                    activity.processFinish(1);
                }
            }
        }
    }
}