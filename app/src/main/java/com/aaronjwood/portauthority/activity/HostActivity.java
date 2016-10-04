package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public abstract class HostActivity extends AppCompatActivity implements HostAsyncResponse {

    protected int layout;
    protected ArrayAdapter<String> adapter;
    protected ListView portList;
    protected ArrayList<String> ports = new ArrayList<>();
    protected ProgressDialog scanProgressDialog;
    protected Dialog portRangeDialog;
    protected int scanProgress;
    private Database db;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(this.layout);

        db = new Database(this);
        setupPortsAdapter();
    }

    /**
     * Sets up the adapter to handle discovered ports
     */
    private void setupPortsAdapter() {
        this.portList = (ListView) findViewById(R.id.portList);
        this.adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.port_list_item, ports);
        this.portList.setAdapter(this.adapter);
    }

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
     * Clean up
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null) {
            db.close();
        }
    }

    /**
     * Save the state of the activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putStringArrayList("ports", ports);
    }

    /**
     * Restore saved data
     *
     * @param savedInstanceState Data from a saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ports = savedInstanceState.getStringArrayList("ports");

        this.setupPortsAdapter();
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

                Host.scanPorts(ip, startPort, stopPort, activity);
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
                if (item == null) {
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
    public void processFinish(SparseArray<String> output) {
        int scannedPort = output.keyAt(0);
        String item = String.valueOf(scannedPort);

        Cursor cursor = db.queryDatabase("SELECT name, port FROM ports WHERE port = ?", new String[]{Integer.toString(scannedPort)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    name = (name.isEmpty()) ? "unknown" : name;
                    item = this.formatOpenPort(output, scannedPort, name, item);
                    this.addOpenPort(item);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Formats a found open port with its name, description, and associated visualization
     *
     * @param entry       Structure holding information about the found open port with its description
     * @param scannedPort The port number
     * @param portName    Friendly name for the port
     * @param item        Contains the transformed output for the open port
     * @return If all associated data is found a port along with its description, underlying service, and visualization is constructed
     */
    private String formatOpenPort(SparseArray<String> entry, int scannedPort, String portName, String item) {
        item = item + " - " + portName;
        if (entry.get(scannedPort) != null) {
            item += " (" + entry.get(scannedPort) + ")";
        }

        //If the port is in any way related to HTTP then present a nice globe icon next to it via unicode
        if (scannedPort == 80 || scannedPort == 443 || scannedPort == 8080) {
            item += " \uD83C\uDF0E";
        }

        return item;
    }

    /**
     * Adds an open port that was found on a host to the list
     *
     * @param port Port number and description
     */
    private void addOpenPort(final String port) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                synchronized (ports) {
                    ports.add(port);

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
