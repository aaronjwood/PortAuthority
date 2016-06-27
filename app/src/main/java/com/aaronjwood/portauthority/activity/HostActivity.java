package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public abstract class HostActivity extends AppCompatActivity implements HostAsyncResponse {

    protected Host host = new Host();
    protected ArrayAdapter<String> adapter;
    protected ListView portList;
    protected ArrayList<String> ports = new ArrayList<>();
    protected ProgressDialog scanProgressDialog;
    protected Dialog portRangeDialog;
    protected int scanProgress;

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
     * Event handler for when the port range reset is triggered
     *
     * @param start Starting port picker
     * @param stop  Stopping port picker
     */
    protected void resetPortRangeScanClick(final NumberPicker start, final NumberPicker stop) {
        portRangeDialog.findViewById(R.id.resetPortRangeScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setValue(Constants.MIN_PORT_VALUE);
                stop.setValue(Constants.MAX_PORT_VALUE);
            }
        });
    }

    /**
     * Event handler for when the port range scan is finally initiated
     *
     * @param start Starting port picker
     * @param stop  Stopping port picker
     */
    protected void startPortRangeScanClick(final NumberPicker start, final NumberPicker stop, final HostActivity activity, final String ip) {
        Button startPortRangeScan = (Button) portRangeDialog.findViewById(R.id.startPortRangeScan);
        startPortRangeScan.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for starting a port range scan
             * @param v
             */
            @Override
            public void onClick(View v) {
                start.clearFocus();
                stop.clearFocus();

                int startPort = start.getValue();
                int stopPort = stop.getValue();
                if ((startPort - stopPort >= 0)) {
                    Toast.makeText(getApplicationContext(), "Please pick a valid port range", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserPreference.savePortRangeStart(activity, startPort);
                UserPreference.savePortRangeHigh(activity, stopPort);

                ports.clear();

                scanProgressDialog = new ProgressDialog(activity, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(stopPort - startPort + 1);
                scanProgressDialog.show();

                host.scanPorts(ip, startPort, stopPort, activity);
            }
        });
    }

    /**
     * Event handler for when an item on the port list is clicked
     */
    protected void portListClick(final String ip) {
        this.portList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler to open certain ports to the browser
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) portList.getItemAtPosition(position);
                if(item == null) {
                    return;
                }

                if (item.contains("80 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ip)));
                }

                if (item.contains("443 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + ip)));
                }

                if (item.contains("8080 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ip + ":8080")));
                }
            }
        });
    }

    /**
     * Delegate to handle incrementing the scan progress dialog
     *
     * @param output The amount of progress to increment
     */
    @Override
    public void processFinish(final int output) {
        this.scanProgress += output;

        if (this.scanProgress % 75 != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (scanProgressDialog != null) {
                    scanProgressDialog.setProgress(scanProgress);
                }
            }
        });
    }

    /**
     * Delegate to handle open ports
     *
     * @param output Contains the port number and associated banner (if any)
     */
    @Override
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
                            synchronized (ports) {
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
                        }
                    });

                    //Make sure to return so that we don't fall through and add the port again!
                    return;
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error reading from port data file!", Toast.LENGTH_SHORT).show();
            return;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to clean up port data file resource", Toast.LENGTH_SHORT).show();
            }
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
                synchronized (ports) {
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
            }
        });
    }
}
