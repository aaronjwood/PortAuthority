package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public abstract class HostActivity extends AppCompatActivity {

    protected ArrayAdapter<String> adapter;
    protected ArrayList<String> ports = new ArrayList<>();
    protected ProgressDialog scanProgressDialog;
    protected Dialog portRangeDialog;

    /**
     * Activity paused
     */
    @Override
    public void onPause() {
        super.onPause();

        if (this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
        }
        if (this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
            this.portRangeDialog.dismiss();
        }
        this.scanProgressDialog = null;
        this.portRangeDialog = null;
    }

    /**
     * Delegate to handle open ports
     *
     * @param output Contains the port number and associated banner (if any)
     */
    public void processFinish(Map<Integer, String> output) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("ports.csv")));
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Can't open port data file!", Toast.LENGTH_SHORT).show();
            return;
        }
        String line;
        int scannedPort = output.keySet().iterator().next();
        String item = String.valueOf(scannedPort);

        try {
            while ((line = reader.readLine()) != null) {
                String[] portInfo = line.split(",");
                String name;
                String port;

                if (portInfo.length > 2) {
                    name = portInfo[0];
                    port = portInfo[1];
                } else {
                    name = "unknown";
                    port = null;
                }

                if (name.isEmpty()) {
                    name = "unknown";
                }

                int filePort;

                //Watch out for inconsistent formatting of the CSV file we're reading!
                try {
                    filePort = Integer.parseInt(port);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (scannedPort == filePort) {
                    item = item + " - " + name;
                    if (output.get(scannedPort) != null) {
                        item += " (" + output.get(scannedPort) + ")";
                    }

                    if (scannedPort == 80 || scannedPort == 443 || scannedPort == 8080) {
                        item += " \uD83C\uDF0E";
                    }

                    final String finalItem = item;

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ports.add(finalItem);

                            Collections.sort(ports, new Comparator<String>() {

                                @Override
                                public int compare(String lhs, String rhs) {
                                    int left = Integer.parseInt(lhs.substring(0, lhs.indexOf("-") - 1));
                                    int right = Integer.parseInt(rhs.substring(0, rhs.indexOf("-") - 1));

                                    return left - right;
                                }
                            });

                            adapter.notifyDataSetChanged();
                        }
                    });

                    reader.close();

                    //Make sure to return so that we don't fall through and add the port again!
                    return;
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error reading from port data file!", Toast.LENGTH_SHORT).show();
            return;
        }

        //If a port couldn't be found in the port data file then make sure it's still caught and added to the list of open ports
        item = item + " - unknown";
        if (output.get(scannedPort) != null) {
            item += " (" + output.get(scannedPort) + ")";
        }

        if (scannedPort == 80 || scannedPort == 443 || scannedPort == 8080) {
            item += " \uD83C\uDF0E";
        }

        final String finalItem = item;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ports.add(finalItem);

                Collections.sort(ports, new Comparator<String>() {

                    @Override
                    public int compare(String lhs, String rhs) {
                        int left = Integer.parseInt(lhs.substring(0, lhs.indexOf("-") - 1));
                        int right = Integer.parseInt(rhs.substring(0, rhs.indexOf("-") - 1));

                        return left - right;
                    }
                });

                adapter.notifyDataSetChanged();
            }
        });
    }
}
