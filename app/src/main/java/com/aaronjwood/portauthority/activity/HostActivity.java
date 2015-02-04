package com.aaronjwood.portauthority.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


public class HostActivity extends Activity implements HostAsyncResponse {

    private static final String TAG = "HostActivity";

    private Wireless wifi;
    private Host host = new Host();
    private TextView hostIpLabel;
    private TextView hostNameLabel;
    private String hostName;
    private String hostIp;
    private TextView hostMacLabel;
    private String hostMac;
    private Button scanWellKnownPortsButton;
    private Button scanPortRangeButton;
    private ListView portList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> ports = new ArrayList<>();
    private ProgressDialog scanProgressDialog;
    private Dialog portRangeDialog;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        this.hostIpLabel = (TextView) findViewById(R.id.hostIpLabel);
        this.hostNameLabel = (TextView) findViewById(R.id.hostName);
        this.scanWellKnownPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        this.scanPortRangeButton = (Button) findViewById(R.id.scanPortRange);
        this.portList = (ListView) findViewById(R.id.portList);
        this.hostMacLabel = (TextView) findViewById(R.id.hostMac);

        if(savedInstanceState != null) {
            this.hostIp = savedInstanceState.getString("hostIp");
            this.hostMac = savedInstanceState.getString("hostMac");
            this.hostName = savedInstanceState.getString("hostName");
            this.ports = savedInstanceState.getStringArrayList("ports");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            this.portList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }
        else if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            this.hostIp = extras.getString("HOST");
            this.hostMac = extras.getString("MAC");

            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            this.portList.setAdapter(adapter);
        }

        this.wifi = new Wireless(this);

        this.host.getHostname(this.hostIp, this);

        this.hostIpLabel.setText(this.hostIp);
        this.hostMacLabel.setText(this.hostMac);

        this.scanWellKnownPortsButton.setOnClickListener(new View.OnClickListener() {

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
                scanProgressDialog.setProgressStyle(scanProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                host.scanPorts(hostIp, 1, 1024, HostActivity.this);
            }
        });

        this.scanPortRangeButton.setOnClickListener(new View.OnClickListener() {

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
                        scanProgressDialog.setProgressStyle(scanProgressDialog.STYLE_HORIZONTAL);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_host, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Delegate to handle incrementing the scan progress dialog
     *
     * @param output The amount of progress to increment
     */
    @Override
    public void processFinish(final int output) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(scanProgressDialog != null && scanProgressDialog.isShowing()) {
                    scanProgressDialog.incrementProgressBy(output);
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
        if(output && this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
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
        try {
            int scannedPort = output.keySet().iterator().next();
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("ports.csv")));
            String line;
            String item = String.valueOf(scannedPort);

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

                try {
                    if(scannedPort == Integer.parseInt(port)) {
                        item = item + " - " + name;
                        if(output.get(scannedPort) != null) {
                            item += " (" + output.get(scannedPort) + ")";
                        }
                        ports.add(item);
                        Collections.sort(ports);

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });

                        reader.close();
                        break;
                    }
                }
                catch(NumberFormatException e) {
                    continue;
                }
            }
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Delegate to handle setting the hostname in the UI
     *
     * @param output Hostname
     */
    @Override
    public void processFinish(String output) {
        this.hostNameLabel.setText(output);
    }
}
