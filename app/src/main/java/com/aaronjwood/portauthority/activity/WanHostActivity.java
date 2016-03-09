package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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


public class WanHostActivity extends AppCompatActivity implements HostAsyncResponse {
    private Host host = new Host();
    private EditText wanHost;
    private ListView portList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> ports = new ArrayList<>();
    private ProgressDialog scanProgressDialog;
    private Dialog portRangeDialog;
    private int scanProgress;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wanhost);

        this.wanHost = (EditText) findViewById(R.id.hostAddress);
        this.portList = (ListView) findViewById(R.id.portList);


        if (savedInstanceState != null) {
            this.ports = savedInstanceState.getStringArrayList("ports");
        } else {
            this.wanHost.setText(UserPreference.getLastUsedHostAddress(this));
        }

        this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
        this.portList.setAdapter(adapter);

        this.setupPortScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserPreference.saveLastUsedHostAddress(this, this.wanHost.getText().toString());
    }

    /**
     * Sets up event handlers and functionality for various port scanning features
     */
    private void setupPortScan() {
        Button scanWellKnownPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        Button scanPortRangeButton = (Button) findViewById(R.id.scanPortRange);

        scanWellKnownPortsButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning well known ports
             * @param v
             */
            @Override
            public void onClick(View v) {
                WanHostActivity.this.ports.clear();

                scanProgressDialog = new ProgressDialog(WanHostActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Well Known Ports");
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                host.scanPorts(wanHost.getText().toString(), 1, 1024, WanHostActivity.this);
            }
        });

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

                if (item.contains("80 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + wanHost.getText().toString())));
                }

                if (item.contains("443 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + wanHost.getText().toString())));
                }

                if (item.contains("8080 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + wanHost.getText().toString() + ":8080")));
                }
            }
        });

        scanPortRangeButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning a port range
             * @param v
             */
            @Override
            public void onClick(View v) {
                WanHostActivity.this.portRangeDialog = new Dialog(WanHostActivity.this, R.style.DialogTheme);
                portRangeDialog.setTitle("Select Port Range");
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                final NumberPicker portRangePickerStart = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStart);
                final NumberPicker portRangePickerStop = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStop);
                Button startPortRangeScan = (Button) portRangeDialog.findViewById(R.id.startPortRangeScan);

                portRangePickerStart.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStart.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStart.setValue(UserPreference.getPortRangeStart(WanHostActivity.this));
                portRangePickerStart.setWrapSelectorWheel(false);
                portRangePickerStop.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStop.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStop.setValue(UserPreference.getPortRangeHigh(WanHostActivity.this));
                portRangePickerStop.setWrapSelectorWheel(false);

                startPortRangeScan.setOnClickListener(new View.OnClickListener() {

                    /**
                     * Click handler for starting a port range scan
                     * @param v
                     */
                    @Override
                    public void onClick(View v) {
                        int startPort = portRangePickerStart.getValue();
                        int stopPort = portRangePickerStop.getValue();
                        if ((startPort - stopPort >= 0)) {
                            Toast.makeText(getApplicationContext(), "Please pick a valid port range", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        UserPreference.savePortRangeStart(WanHostActivity.this, startPort);
                        UserPreference.savePortRangeHigh(WanHostActivity.this, stopPort);

                        WanHostActivity.this.ports.clear();

                        scanProgressDialog = new ProgressDialog(WanHostActivity.this, R.style.DialogTheme);
                        scanProgressDialog.setCancelable(false);
                        scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                        scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        scanProgressDialog.setProgress(0);
                        scanProgressDialog.setMax(stopPort - startPort + 1);
                        scanProgressDialog.show();

                        host.scanPorts(wanHost.getText().toString(), startPort, stopPort, WanHostActivity.this);
                    }
                });

                portRangeDialog.findViewById(R.id.resetPortRangeScan).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        portRangePickerStart.setValue(Constants.MIN_PORT_VALUE);
                        portRangePickerStop.setValue(Constants.MAX_PORT_VALUE);
                    }
                });
            }
        });
    }

    /**
     * Save the state of the activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putStringArrayList("ports", this.ports);
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
     * Delegate to handle incrementing the scan progress dialog
     *
     * @param output The amount of progress to increment
     */
    @Override
    public void processFinish(final int output) {
        this.scanProgress += output;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (scanProgressDialog != null && scanProgressDialog.isShowing() && scanProgress % 50 == 0) {
                    scanProgressDialog.setProgress(scanProgress);
                }
            }
        });
    }

    /**
     * Delegate to determine if the progress dialog should be dismissed or not
     *
     * @param output True if the dialog should be dismissed
     */
    @Override
    public void processFinish(boolean output) {
        if (this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
            this.scanProgress = 0;
        }
        if (this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
            this.portRangeDialog.dismiss();
        }
        if (!output) {
            runOnUiThread(new Runnable() {

                /**
                 * Indicate to the user that they've entered in something wrong
                 */
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Please enter a valid URL or IP address", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
