package com.aaronjwood.portauthority.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.utils.Constants;
import com.aaronjwood.portauthority.utils.UserPreference;


public class WanHostActivity extends HostActivity {

    private EditText wanHost;

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
            ports = savedInstanceState.getStringArrayList("ports");
        } else {
            this.wanHost.setText(UserPreference.getLastUsedHostAddress(this));
        }

        this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, ports);
        this.portList.setAdapter(adapter);

        this.setupPortScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserPreference.saveLastUsedHostAddress(this, this.wanHost.getText().toString());
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
                ports.clear();

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
                portRangeDialog = new Dialog(WanHostActivity.this, R.style.DialogTheme);
                portRangeDialog.setTitle("Select Port Range");
                portRangeDialog.setContentView(R.layout.port_range);
                portRangeDialog.show();

                NumberPicker portRangePickerStart = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStart);
                NumberPicker portRangePickerStop = (NumberPicker) portRangeDialog.findViewById(R.id.portRangePickerStop);

                portRangePickerStart.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStart.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStart.setValue(UserPreference.getPortRangeStart(WanHostActivity.this));
                portRangePickerStart.setWrapSelectorWheel(false);
                portRangePickerStop.setMinValue(Constants.MIN_PORT_VALUE);
                portRangePickerStop.setMaxValue(Constants.MAX_PORT_VALUE);
                portRangePickerStop.setValue(UserPreference.getPortRangeHigh(WanHostActivity.this));
                portRangePickerStop.setWrapSelectorWheel(false);

                startPortRangeScanClick(portRangePickerStart, portRangePickerStop, WanHostActivity.this, wanHost.getText().toString());
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
        this.portListClick(wanHost.getText().toString());
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
}
