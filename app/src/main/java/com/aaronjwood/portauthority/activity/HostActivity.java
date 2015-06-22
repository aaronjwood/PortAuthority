package com.aaronjwood.portauthority.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


public class HostActivity extends Activity implements HostAsyncResponse {

    private static final String TAG = "HostActivity";

    private Wireless wifi;
    private Host host = new Host();
    private TextView hostNameLabel;
    private String hostName;
    private String hostIp;
    private String hostMac;
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
        setContentView(R.layout.activity_host);

        TextView hostIpLabel = (TextView) findViewById(R.id.hostIpLabel);
        this.hostNameLabel = (TextView) findViewById(R.id.hostName);
        TextView hostMacVendor = (TextView) findViewById(R.id.hostMacVendor);
        Button scanWellKnownPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        Button scanPortRangeButton = (Button) findViewById(R.id.scanPortRange);
        ListView portList = (ListView) findViewById(R.id.portList);
        TextView hostMacLabel = (TextView) findViewById(R.id.hostMac);

        if(savedInstanceState != null) {
            this.hostIp = savedInstanceState.getString("hostIp");
            this.hostMac = savedInstanceState.getString("hostMac");
            this.hostName = savedInstanceState.getString("hostName");
            this.ports = savedInstanceState.getStringArrayList("ports");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            portList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }
        else if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            this.hostIp = extras.getString("HOST");
            this.hostMac = extras.getString("MAC");

            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            portList.setAdapter(adapter);
        }

        this.wifi = new Wireless(this);

        this.host.getHostname(this.hostIp, this);

        String mac = this.hostMac.replace(":", "");
        mac = mac.substring(0, 6);

        hostMacVendor.setText(this.host.getMacVendor(mac, this));

        hostIpLabel.setText(this.hostIp);
        hostMacLabel.setText(this.hostMac);

        scanWellKnownPortsButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning well known ports
             * @param v
             */
            @Override
            public void onClick(View v) {
                if(!wifi.isConnected()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                HostActivity.this.ports.clear();

                scanProgressDialog = new ProgressDialog(HostActivity.this);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Well Known Ports");
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                host.scanPorts(hostIp, 1, 1024, HostActivity.this);
            }
        });

        scanPortRangeButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning a port range
             * @param v
             */
            @Override
            public void onClick(View v) {
                if(!wifi.isConnected()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                HostActivity.this.portRangeDialog = new Dialog(HostActivity.this);
                portRangeDialog.setTitle("Select Port Range");
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                final NumberPicker portRangePickerStart = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStart);
                final NumberPicker portRangePickerStop = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStop);
                Button startPortRangeScan = (Button) portRangeDialog.findViewById(R.id.startPortRangeScan);

                portRangePickerStart.setMinValue(1);
                portRangePickerStart.setMaxValue(65535);
                portRangePickerStart.setWrapSelectorWheel(false);
                portRangePickerStop.setMinValue(1);
                portRangePickerStop.setMaxValue(65535);
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

                        if((startPort - stopPort >= 0)) {
                            Toast.makeText(getApplicationContext(), "Please pick a valid port range", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        HostActivity.this.ports.clear();

                        scanProgressDialog = new ProgressDialog(HostActivity.this);
                        scanProgressDialog.setCancelable(false);
                        scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                        scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        scanProgressDialog.setProgress(0);
                        scanProgressDialog.setMax(stopPort - startPort + 1);
                        scanProgressDialog.show();

                        host.scanPorts(hostIp, startPort, stopPort, HostActivity.this);
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

        savedState.putString("hostIp", this.hostIp);
        savedState.putString("hostName", this.hostName);
        savedState.putString("hostMac", this.hostMac);
        savedState.putStringArrayList("ports", this.ports);
    }

    /**
     * Activity paused
     */
    @Override
    public void onPause() {
        super.onPause();

        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
        }
        if(this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
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
        if(scanProgressDialog != null && scanProgressDialog.isShowing() && this.scanProgress % 50 == 0) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    scanProgressDialog.setProgress(scanProgress);
                }
            });
        }
    }

    /**
     * Delegate to determine if the progress dialog should be dismissed or not
     *
     * @param output True if the dialog should be dismissed
     */
    @Override
    public void processFinish(boolean output) {
        if(output && this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
            this.scanProgress = 0;
        }
        if(output && this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
            this.portRangeDialog.dismiss();
        }
    }

    /**
     * Delegate to handle open ports
     *
     * @param output Contains the port number and associated banner (if any)
     */
    @Override
    public void processFinish(Map<Integer, String> output) {
        int scannedPort = output.keySet().iterator().next();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("ports.csv")));
        }
        catch(IOException e) {
            Toast.makeText(getApplicationContext(), "Can't open port data file!", Toast.LENGTH_SHORT).show();
            return;
        }
        String line;
        String item = String.valueOf(scannedPort);

        try {
            while((line = reader.readLine()) != null) {
                String[] portInfo = line.split(",");
                String name;
                String port;

                if(portInfo.length > 2) {
                    name = portInfo[0];
                    port = portInfo[1];
                }
                else {
                    name = "unknown";
                    port = null;
                }

                int filePort;

                //Watch out for inconsistent formatting of the CSV file we're reading!
                try {
                    filePort = Integer.parseInt(port);
                }
                catch(NumberFormatException e) {
                    continue;
                }

                if(scannedPort == filePort) {
                    item = item + " - " + name;
                    if(output.get(scannedPort) != null) {
                        item += " (" + output.get(scannedPort) + ")";
                    }

                    final String finalItem = item;
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ports.add(finalItem);
                            Collections.sort(ports);
                            adapter.notifyDataSetChanged();
                        }
                    });

                    reader.close();

                    //Make sure to return so that we don't fall through and add the port again!
                    return;
                }
            }
        }
        catch(IOException e) {
            Toast.makeText(getApplicationContext(), "Error reading from port data file!", Toast.LENGTH_SHORT).show();
            return;
        }

        //If a port couldn't be found in the port data file then make sure it's still caught and added to the list of open ports
        item = item + " - unknown";
        if(output.get(scannedPort) != null) {
            item += " (" + output.get(scannedPort) + ")";
        }

        final String finalItem = item;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ports.add(finalItem);
                Collections.sort(ports);
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Delegate to handle setting the hostname in the UI
     *
     * @param output Hostname
     */
    @Override
    public void processFinish(String output) {
        if(output != null) {
            this.hostNameLabel.setText(output);
        }
        else {
            this.hostNameLabel.setText("Couldn't get hostname");
        }
    }
}
