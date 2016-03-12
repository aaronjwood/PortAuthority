package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;


public class LanHostActivity extends HostActivity {
    private Wireless wifi;
    private Host host = new Host();
    private String hostName;
    private String hostIp;
    private String hostMac;
    private ListView portList;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lanhost);

        TextView hostIpLabel = (TextView) findViewById(R.id.hostIpLabel);
        TextView hostMacVendor = (TextView) findViewById(R.id.hostMacVendor);
        TextView hostMacLabel = (TextView) findViewById(R.id.hostMac);
        this.portList = (ListView) findViewById(R.id.portList);

        if (savedInstanceState != null) {
            this.hostName = savedInstanceState.getString("hostName");
            this.hostIp = savedInstanceState.getString("hostIp");
            this.hostMac = savedInstanceState.getString("hostMac");
            this.ports = savedInstanceState.getStringArrayList("ports");
        } else if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            this.hostName = extras.getString("HOSTNAME");
            this.hostIp = extras.getString("IP");
            this.hostMac = extras.getString("MAC");
        }

        this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
        this.portList.setAdapter(adapter);
        this.wifi = new Wireless(this);

        hostMacVendor.setText(this.host.getMacVendor(hostMac.replace(":", "").substring(0, 6), this));

        hostIpLabel.setText(this.hostName);
        hostMacLabel.setText(this.hostMac);

        this.setupPortScan();
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
                if (!wifi.isConnected()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LanHostActivity.this.ports.clear();

                scanProgressDialog = new ProgressDialog(LanHostActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Well Known Ports");
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                host.scanPorts(hostIp, 1, 1024, LanHostActivity.this);
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
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + hostIp)));
                }

                if (item.contains("443 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + hostIp)));
                }

                if (item.contains("8080 -")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + hostIp + ":8080")));
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
                if (!wifi.isConnected()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LanHostActivity.this.portRangeDialog = new Dialog(LanHostActivity.this, R.style.DialogTheme);
                portRangeDialog.setTitle("Select Port Range");
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                final NumberPicker portRangePickerStart = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStart);
                final NumberPicker portRangePickerStop = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStop);
                Button startPortRangeScan = (Button) portRangeDialog.findViewById(R.id.startPortRangeScan);

                portRangePickerStart.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStart.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStart.setValue(UserPreference.getPortRangeStart(LanHostActivity.this));
                portRangePickerStart.setWrapSelectorWheel(false);
                portRangePickerStop.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStop.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStop.setValue(UserPreference.getPortRangeHigh(LanHostActivity.this));
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

                        UserPreference.savePortRangeStart(LanHostActivity.this, startPort);
                        UserPreference.savePortRangeHigh(LanHostActivity.this, stopPort);

                        LanHostActivity.this.ports.clear();

                        scanProgressDialog = new ProgressDialog(LanHostActivity.this, R.style.DialogTheme);
                        scanProgressDialog.setCancelable(false);
                        scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                        scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        scanProgressDialog.setProgress(0);
                        scanProgressDialog.setMax(stopPort - startPort + 1);
                        scanProgressDialog.show();

                        host.scanPorts(hostIp, startPort, stopPort, LanHostActivity.this);
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

        savedState.putString("hostName", this.hostName);
        savedState.putString("hostIp", this.hostIp);
        savedState.putString("hostMac", this.hostMac);
        savedState.putStringArrayList("ports", this.ports);
    }


    /**
     * Delegate to determine if the progress dialog should be dismissed or not
     *
     * @param output True if the dialog should be dismissed
     */
    @Override
    public void processFinish(boolean output) {
        if (output && this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
            this.scanProgress = 0;
        }
        if (output && this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
            this.portRangeDialog.dismiss();
        }
    }
}
