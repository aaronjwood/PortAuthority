package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.listener.ScanPortsListener;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Network;
import com.aaronjwood.portauthority.network.Wired;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.response.HostAsyncResponse;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.Errors;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class HostActivity extends AppCompatActivity implements HostAsyncResponse {

    protected Wireless wifi;
    protected Wired wired;
    protected int layout;
    protected ArrayAdapter<String> adapter;
    protected ListView portList;
    protected final List<String> ports = Collections.synchronizedList(new ArrayList<String>());
    protected ProgressDialog scanProgressDialog;
    protected Dialog portRangeDialog;
    protected Handler handler;
    private Database db;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout);

        Context ctx = getApplicationContext();
        db = Database.getInstance(ctx);
        wifi = new Wireless(ctx);
        wired = new Wired(ctx);
        handler = new Handler(Looper.getMainLooper());

        setupPortsAdapter();
    }

    /**
     * Sets up animations for the activity
     */
    protected void setAnimations() {
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_slide_in_bottom);
        portList.setLayoutAnimation(animation);
    }

    /**
     * Sets up the adapter to handle discovered ports
     */
    private void setupPortsAdapter() {
        portList = findViewById(R.id.portList);
        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.port_list_item, ports);
        portList.setAdapter(adapter);
        setAnimations();
    }

    /**
     * Activity paused
     */
    @Override
    public void onPause() {
        super.onPause();

        if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.dismiss();
        }
        if (portRangeDialog != null && portRangeDialog.isShowing()) {
            portRangeDialog.dismiss();
        }
        scanProgressDialog = null;
        portRangeDialog = null;
    }

    /**
     * Clean up
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Save the state of the activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        String[] savedList = ports.toArray(new String[ports.size()]);
        savedState.putStringArray("ports", savedList);
    }

    /**
     * Restore saved data
     *
     * @param savedInstanceState Data from a saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String[] savedList = savedInstanceState.getStringArray("ports");
        if (savedList != null) {
            ports.addAll(Arrays.asList(savedList));
        }

        setupPortsAdapter();
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
     * @param start    Starting port picker
     * @param stop     Stopping port picker
     * @param timeout  Socket timeout
     * @param activity Calling activity
     * @param ip       IP address
     */
    protected void startPortRangeScanClick(final NumberPicker start, final NumberPicker stop, final int timeout, final HostActivity activity, final String ip) {
        Button startPortRangeScan = portRangeDialog.findViewById(R.id.startPortRangeScan);
        startPortRangeScan.setOnClickListener(new ScanPortsListener(ports, adapter) {

            /**
             * Click handler for starting a port range scan
             * @param v
             */
            @Override
            public void onClick(View v) {
                super.onClick(v);

                start.clearFocus();
                stop.clearFocus();

                int startPort = start.getValue();
                int stopPort = stop.getValue();
                if ((startPort - stopPort > 0)) {
                    Toast.makeText(getApplicationContext(), "Please pick a valid port range", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserPreference.savePortRangeStart(activity, startPort);
                UserPreference.savePortRangeHigh(activity, stopPort);

                scanProgressDialog = new ProgressDialog(activity, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(stopPort - startPort + 1);
                scanProgressDialog.show();

                Host.scanPorts(ip, startPort, stopPort, timeout, activity);
            }
        });
    }

    /**
     * Event handler for when an item on the port list is clicked
     */
    protected void portListClick(final String ip) {
        portList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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

                Intent intent = null;

                if (item.contains("80 -")) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ip));
                }

                if (item.contains("443 -")) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + ip));
                }

                if (item.contains("8080 -")) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ip + ":8080"));
                }

                PackageManager packageManager = getPackageManager();
                if (intent != null && packageManager != null) {
                    if (packageManager.resolveActivity(intent, 0) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "No application found to open this to the browser!", Toast.LENGTH_SHORT).show();
                    }
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
        handler.post(new Runnable() {

            @Override
            public void run() {
                if (scanProgressDialog != null) {
                    scanProgressDialog.incrementProgressBy(output);
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

        String name = db.selectPortDescription(String.valueOf(scannedPort));
        name = (name.isEmpty()) ? "unknown" : name;
        item = formatOpenPort(output, scannedPort, name, item);
        addOpenPort(item);
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
        String data = item + " - " + portName;
        if (entry.get(scannedPort) != null) {
            data += " (" + entry.get(scannedPort) + ")";
        }

        //If the port is in any way related to HTTP then present a nice globe icon next to it via unicode
        if (scannedPort == 80 || scannedPort == 443 || scannedPort == 8080) {
            data += " \uD83C\uDF0E";
        }

        return data;
    }

    /**
     * Adds an open port that was found on a host to the list
     *
     * @param port Port number and description
     */
    private void addOpenPort(final String port) {
        setAnimations();
        handler.post(new Runnable() {

            @Override
            public void run() {
                ports.add(port);
                Collections.sort(ports, new Comparator<String>() {

                    @Override
                    public int compare(String lhs, String rhs) {
                        int left = Integer.parseInt(lhs.substring(0, lhs.indexOf('-') - 1));
                        int right = Integer.parseInt(rhs.substring(0, rhs.indexOf('-') - 1));

                        return left - right;
                    }
                });

                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Delegate to handle bubbled up errors
     *
     * @param output The exception we want to handle
     * @param <T>    Exception
     */
    public <T extends Throwable> void processFinish(final T output) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Errors.showError(getApplicationContext(), output.getLocalizedMessage());
            }
        });
    }

    /**
     * Determines if there is an active network connection.
     *
     * @return True if there's a connection, otherwise false.
     */
    protected boolean isConnected() {
        try {
            return wifi.isConnected() || wired.isConnected();
        } catch (Network.NoConnectivityManagerException e) {
            return false;
        }
    }
}
