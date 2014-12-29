package com.aaronjwood.portauthority;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.aaronjwood.portauthority.Network.Host;

import java.util.ArrayList;


public class HostActivity extends Activity {

    private Host host;
    private TextView hostIpLabel;
    private String hostIp;
    private Button scanPortsButton;
    private ListView portList;
    private ArrayAdapter<Integer> adapter;
    private ArrayList<Integer> ports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        this.hostIpLabel = (TextView) findViewById(R.id.hostIpLabel);
        this.scanPortsButton = (Button) findViewById(R.id.scanWellKnownPorts);
        this.portList = (ListView) findViewById(R.id.portList);

        if(savedInstanceState != null) {
            this.hostIp = savedInstanceState.getString("hostIp");
            this.ports = savedInstanceState.getIntegerArrayList("ports");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.ports);
            this.portList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }
        else if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            this.hostIp = extras.getString("HOST");
        }

        this.hostIpLabel.setText(this.hostIp);

        this.host = new Host(this, this.hostIp);
        this.host.getHostName();
        this.host.getMacAddress();

        this.scanPortsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ports.clear();

                adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, ports);
                portList.setAdapter(adapter);

                host.scanSystemPorts();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putString("hostIp", this.hostIp);
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
}
