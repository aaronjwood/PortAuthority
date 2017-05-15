package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.io.IOException;

public final class LanHostActivity extends HostActivity {
    private Wireless wifi;
    private Host host;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.layout = R.layout.activity_lanhost;
        super.onCreate(savedInstanceState);

        TextView hostName = (TextView) findViewById(R.id.hostName);
        TextView hostMacVendor = (TextView) findViewById(R.id.hostMacVendor);
        TextView hostMac = (TextView) findViewById(R.id.hostMac);
        TextView ipAddress = (TextView) findViewById(R.id.ipAddress);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        this.wifi = new Wireless(getApplicationContext());
        this.host = (Host) extras.get("HOST");
        if (this.host == null) {
            return;
        }

        hostMacVendor.setText(Host.getMacVendor(this.host.getMac().replace(":", "").substring(0, 6), this));
        hostName.setText(this.host.getHostname());
        hostMac.setText(this.host.getMac());
        ipAddress.setText(this.host.getIp());

        this.setupPortScan();
        this.setupWol();
    }

    /**
     * Save the state of the activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putSerializable("host", this.host);
    }

    /**
     * Restore saved data
     *
     * @param savedInstanceState Data from a saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        this.host = (Host) savedInstanceState.get("host");
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
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notConnectedLan), Toast.LENGTH_SHORT).show();
                    return;
                }

                ports.clear();

                int startPort = 1;
                int stopPort = 1024;
                scanProgressDialog = new ProgressDialog(LanHostActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Port " + startPort + " to " + stopPort);
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                Host.scanPorts(host.getIp(), startPort, stopPort, UserPreference.getLanSocketTimeout(getApplicationContext()), LanHostActivity.this);
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
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notConnectedLan), Toast.LENGTH_SHORT).show();
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

                startPortRangeScanClick(portRangePickerStart, portRangePickerStop, UserPreference.getLanSocketTimeout(getApplicationContext()), LanHostActivity.this, host.getIp());
                resetPortRangeScanClick(portRangePickerStart, portRangePickerStop);
            }
        });
    }

    private void setupWol() {
        Button wakeUpButton = (Button) findViewById(R.id.wakeOnLan);
        wakeUpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!wifi.isConnectedWifi()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notConnectedLan), Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    host.wakeOnLan();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.waking), host.getHostname()), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sets up event handlers and functionality for various port scanning features
     */
    private void setupPortScan() {
        this.scanWellKnownPortsClick();
        this.scanPortRangeClick();
        this.portListClick(this.host.getIp());
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
        }
        if (output && this.portRangeDialog != null && this.portRangeDialog.isShowing()) {
            this.portRangeDialog.dismiss();
        }
    }
}
