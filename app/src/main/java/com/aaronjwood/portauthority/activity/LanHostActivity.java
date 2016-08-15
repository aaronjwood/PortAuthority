package com.aaronjwood.portauthority.activity;

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
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;


public class LanHostActivity extends HostActivity {
    private Wireless wifi;
    private String hostName;
    private String hostIp;
    private String hostMac;

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
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        this.hostName = extras.getString("HOSTNAME");
        this.hostIp = extras.getString("IP");
        this.hostMac = extras.getString("MAC");

        this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, ports);
        this.portList.setAdapter(adapter);
        this.wifi = new Wireless(this);

        hostMacVendor.setText(this.host.getMacVendor(hostMac.replace(":", "").substring(0, 6), this));

        hostIpLabel.setText(this.hostName);
        hostMacLabel.setText(this.hostMac);

        this.setupPortScan();
    }

    /**
     * Restore saved data
     *
     * @param savedInstanceState Data from a saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        this.hostName = savedInstanceState.getString("hostName");
        this.hostIp = savedInstanceState.getString("hostIp");
        this.hostMac = savedInstanceState.getString("hostMac");
        ports = savedInstanceState.getStringArrayList("ports");
    }

    /**
     * Event handler for when the well known port scan is initiated
     */
    private void scanWellKnownPortsClick() {
        Button scanWellKnownPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        scanWellKnownPortsButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning well known ports
             * @param v
             */
            @Override
            public void onClick(View v) {
                if (!wifi.isConnectedWifi()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                ports.clear();

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
    }

    /**
     * Event handler for when a port range scan is requested
     */
    private void scanPortRangeClick() {
        Button scanPortRangeButton = (Button) findViewById(R.id.scanPortRange);
        scanPortRangeButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning a port range
             * @param v
             */
            @Override
            public void onClick(View v) {
                if (!wifi.isConnectedWifi()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                portRangeDialog = new Dialog(LanHostActivity.this, R.style.DialogTheme);
                portRangeDialog.setTitle("Select Port Range");
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                NumberPicker portRangePickerStart = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStart);
                NumberPicker portRangePickerStop = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStop);

                portRangePickerStart.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStart.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStart.setValue(UserPreference.getPortRangeStart(LanHostActivity.this));
                portRangePickerStart.setWrapSelectorWheel(false);
                portRangePickerStop.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStop.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStop.setValue(UserPreference.getPortRangeHigh(LanHostActivity.this));
                portRangePickerStop.setWrapSelectorWheel(false);

                startPortRangeScanClick(portRangePickerStart, portRangePickerStop, LanHostActivity.this, hostIp);
                resetPortRangeScanClick(portRangePickerStart, portRangePickerStop);
            }
        });
    }

    /**
     * Sets up event handlers and functionality for various port scanning features
     */
    private void setupPortScan() {
        this.scanWellKnownPortsClick();
        this.scanPortRangeClick();
        this.portListClick(hostIp);
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
        savedState.putStringArrayList("ports", ports);
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
