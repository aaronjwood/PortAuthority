package com.aaronjwood.portauthority.runnable;

import android.util.SparseArray;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.StandardCharsets;

public class ScanPortsRunnable implements Runnable {
    private final String ip;
    private final int startPort;
    private final int stopPort;
    private final int timeout;
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

            Socket socket = new Socket();
            try {
                socket.setReuseAddress(true);
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(ip, i), timeout);
            } catch (IllegalBlockingModeException | IllegalArgumentException e) {
                activity.processFinish(e);
                continue;
            } catch (IOException e) {
                activity.processFinish(1);
                continue; // Connection failures mean that the port isn't open.
            }

            SparseArray<String> portData = new SparseArray<>();
            String data = null;
            try {
                InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader buffered = new BufferedReader(input);
                if (i == 22) {
                    data = parseSSH(buffered);
                } else if (i == 80 || i == 443 || i == 8080) {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    data = parseHTTP(buffered, out);
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

    /**
     * Tries to determine the SSH version used.
     *
     * @param reader Reads SSH version from the connected socket
     * @return SSH banner
     * @throws IOException
     */
    private String parseSSH(BufferedReader reader) throws IOException {
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    /**
     * Tries to determine what web server is used
     *
     * @param reader Reads headers to determine server type
     * @param writer Sends HTTP request to get a response to parse
     * @return HTTP banner
     * @throws IOException
     */
    private String parseHTTP(BufferedReader reader, PrintWriter writer) throws IOException {
        writer.println("GET / HTTP/1.1\r\nHost: " + ip + "\r\n");
        char[] buffer = new char[256];
        int bytesRead = reader.read(buffer, 0, buffer.length);
        writer.close();
        reader.close();
        if (bytesRead == 0) {
            return null;
        }

        String data = new String(buffer).toLowerCase();

        if (data.contains("apache") || data.contains("httpd")) {
            return "Apache";
        }

        if (data.contains("iis") || data.contains("microsoft")) {
            return "IIS";
        }

        if (data.contains("nginx")) {
            return "NGINX";
        }

        if (data.contains("node")) {
            return "Node.js";
        }

        if (data.contains("tomcat")) {
            return "Tomcat";
        }

        if (data.contains("litespeed")) {
            return "LiteSpeed";
        }

        return null;
    }
}
