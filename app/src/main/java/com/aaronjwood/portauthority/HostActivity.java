package com.aaronjwood.portauthority;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.util.ArrayList;


public class HostActivity extends Activity implements HostAsyncResponse {

    private Host host = new Host();
    private TextView hostIpLabel;
    private TextView hostNameLabel;
    private String hostName;
    private String hostIp;
    private TextView hostMacLabel;
    private String hostMac;
    private Button scanPortsButton;
    private ListView portList;
    private ArrayAdapter<Integer> adapter;
    private ArrayList<Integer> ports = new ArrayList<>();
    private ProgressDialog scanProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        this.hostIpLabel = (TextView) findViewById(R.id.hostIpLabel);
        this.hostNameLabel = (TextView) findViewById(R.id.hostName);
        this.scanPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        this.portList = (ListView) findViewById(R.id.portList);
        this.hostMacLabel = (TextView) findViewById(R.id.hostMac);

        if(savedInstanceState != null) {
            this.hostIp = savedInstanceState.getString("hostIp");
            this.hostMac = savedInstanceState.getString("hostMac");
            this.hostName = savedInstanceState.getString("hostName");
            this.ports = savedInstanceState.getIntegerArrayList("ports");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            this.portList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }
        else if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            this.hostIp = extras.getString("HOST");
            this.hostMac = extras.getString("MAC");
        }

        this.host.getHostname(this.hostIp, this);

        this.hostIpLabel.setText(this.hostIp);
        this.hostMacLabel.setText(this.hostMac);

        this.scanPortsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanProgressDialog = new ProgressDialog(HostActivity.this, AlertDialog.THEME_HOLO_DARK);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning Well Known Ports");
                scanProgressDialog.setProgressStyle(scanProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(1024);
                scanProgressDialog.show();

                host.scanWellKnownPorts(hostIp, HostActivity.this);
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putString("hostIp", this.hostIp);
        savedState.putString("hostName", this.hostName);
        savedState.putString("hostMac", this.hostMac);
        savedState.putIntegerArrayList("ports", this.ports);
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

    @Override
    public void processFinish(ArrayList<Integer> output) {
        this.scanProgressDialog.dismiss();
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, output);
        this.portList.setAdapter(adapter);
    }

    @Override
    public void processFinish(int output) {
        this.scanProgressDialog.incrementProgressBy(output);
    }

    @Override
    public void processFinish(String output) {
        this.hostNameLabel.setText(output);
    }
}
