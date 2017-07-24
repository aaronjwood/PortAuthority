package com.aaronjwood.portauthority.runnable;

import android.util.SparseArray;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;

public class ScanPortsRunnable implements Runnable {
    private String ip;
    private int startPort;
    private int stopPort;
    private int timeout;
    private final WeakReference<HostAsyncResponse> delegate;

    /**
     * Constructor to set the necessary data to perform a port scan
     *
     * @param ip        IP address
     * @param startPort Port to start scanning at
     * @param stopPort  Port to stop scanning at
     * @param timeout   Socket timeout
     * @param delegate  Called when this chunk of ports has finished scanning
     */
    public ScanPortsRunnable(String ip, int startPort, int stopPort, int timeout, WeakReference<HostAsyncResponse> delegate) {
        this.ip = ip;
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.timeout = timeout;
        this.delegate = delegate;
    }

    /**
     * Starts the port scan
     */
    @Override
    public void run() {
        HostAsyncResponse activity = delegate.get();
        for (int i = startPort; i <= stopPort; i++) {
            if (activity == null) {
                return;
            }

            SparseArray<String> portData = new SparseArray<>();
            BufferedReader in;
            String data = null;
            Socket socket = new Socket();

            try {
                socket.setReuseAddress(true);
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(ip, i), timeout);
            } catch (SocketTimeoutException | IllegalBlockingModeException | IllegalArgumentException e) {
                activity.processFinish(e);
            } catch (IOException e) {
                activity.processFinish(1);
                continue; // Connection failures mean that the port isn't open.
            }

            //TODO: this is a bit messy, refactor and break it up
            try {
                if (i == 22) {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    data = in.readLine();
                    in.close();
                } else if (i == 80 || i == 443 || i == 8080) {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("GET / HTTP/1.1\r\nHost: " + ip + "\r\n");

                    char[] buffer = new char[256];
                    in.read(buffer, 0, buffer.length);
                    out.close();
                    in.close();
                    data = new String(buffer).toLowerCase();
                    if (data.contains("apache") || data.contains("httpd")) {
                        data = "Apache";
                    } else if (data.contains("iis") || data.contains("microsoft")) {
                        data = "IIS";
                    } else if (data.contains("nginx")) {
                        data = "NGINX";
                    } else {
                        data = null;
                    }
                }
            } catch (IOException e) {
                activity.processFinish(e);
            } finally {
                portData.put(i, data);
                activity.processFinish(portData);
                activity.processFinish(1);
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Something's really wrong if we can't close the socket...
                }
            }
        }
    }
}
