package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.listener.ScanPortsListener;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.Errors;
import com.aaronjwood.portauthority.utils.UserPreference;

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
        layout = R.layout.activity_lanhost;
        super.onCreate(savedInstanceState);

        TextView hostName = findViewById(R.id.hostName);
        TextView hostMacVendor = findViewById(R.id.hostMacVendor);
        TextView hostMac = findViewById(R.id.hostMac);
        TextView ipAddress = findViewById(R.id.ipAddress);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        wifi = new Wireless(getApplicationContext());
        host = (Host) extras.get("HOST");
        if (host == null) {
            return;
        }

        hostMacVendor.setText(host.getVendor());
        hostName.setText(host.getHostname());
        hostMac.setText(host.getMac());
        ipAddress.setText(host.getIp());

        setupPortScan();
        setupWol();
    }

    /**
     * Save the state of the activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putSerializable("host", host);
    }

    /**
     * Restore saved data
     *
     * @param savedInstanceState Data from a saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        host = (Host) savedInstanceState.get("host");
    }

    /**
     * Event handler for when the well known port scan is initiated
     */
    private void scanWellKnownPortsClick() {
        Button scanWellKnownPortsButton = findViewById(R.id.scanWellKnownPorts);
        scanWellKnownPortsButton.setOnClickListener(new ScanPortsListener(ports, adapter) {

            /**
             * Click handler for scanning well known ports
             * @param v
             */
            @Override
            public void onClick(View v) {
                super.onClick(v);

                try {
                    if (!wifi.isConnectedWifi()) {
                        Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                        return;
                    }
                } catch (Wireless.NoConnectivityManagerException e) {
                    Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                    return;
                }

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
        Button scanPortRangeButton = findViewById(R.id.scanPortRange);
        scanPortRangeButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler for scanning a port range
             * @param v
             */
            @Override
            public void onClick(View v) {
                try {
                    if (!wifi.isConnectedWifi()) {
                        Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                        return;
                    }
                } catch (Wireless.NoConnectivityManagerException e) {
                    Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                    return;
                }

                portRangeDialog = new Dialog(LanHostActivity.this, R.style.DialogTheme);
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                NumberPicker portRangePickerStart = portRangeDialog.findViewById(R.id.portRangePickerStart);
                NumberPicker portRangePickerStop = portRangeDialog.findViewById(R.id.portRangePickerStop);

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

    /**
     * Event handler for waking up a host via WoL
     */
    private void setupWol() {
        Button wakeUpButton = findViewById(R.id.wakeOnLan);
        wakeUpButton.setOnClickListener(v -> {
            try {
                if (!wifi.isConnectedWifi()) {
                    Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                    return;
                }
            } catch (Wireless.NoConnectivityManagerException e) {
                Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                return;
            }

            host.wakeOnLan();
            Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.waking), host.getHostname()), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Sets up event handlers and functionality for various port scanning features
     */
    private void setupPortScan() {
        scanWellKnownPortsClick();
        scanPortRangeClick();
        portListClick(host.getIp());
    }


    /**
     * Delegate to determine if the progress dialog should be dismissed or not
     *
     * @param output True if the dialog should be dismissed
     */
    @Override
    public void processFinish(boolean output) {
        if (output && scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.dismiss();
        }
        if (output && portRangeDialog != null && portRangeDialog.isShowing()) {
            portRangeDialog.dismiss();
        }
    }
}
