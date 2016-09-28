package com.aaronjwood.portauthority.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Dns;
import com.aaronjwood.portauthority.response.DnsAsyncResponse;
import com.aaronjwood.portauthority.utils.UserPreference;

public final class DnsActivity extends AppCompatActivity implements DnsAsyncResponse {

    private EditText domainName;
    private TextView dnsAnswer;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dns);

        this.domainName = (EditText) findViewById(R.id.domainName);
        this.dnsAnswer = (TextView) findViewById(R.id.dnsAnswer);
        this.domainName.setText(UserPreference.getLastUsedDomainName(this));

        this.dnsLookupClick();
    }

    /**
     * Clean up
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserPreference.saveLastUsedDomainName(this, this.domainName.getText().toString());
    }

    /**
     * Event handler for when a DNS lookup is requested
     */
    private void dnsLookupClick() {
        final EditText domainElement = (EditText) findViewById(R.id.domainName);
        final Spinner recordElement = (Spinner) findViewById(R.id.recordSpinner);
        Button dnsLookupButton = (Button) findViewById(R.id.dnsLookup);
        dnsLookupButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Initiate DNS lookups
             * @param view
             */
            @Override
            public void onClick(View view) {
                String domain = domainElement.getText().toString();
                String recordType = recordElement.getSelectedItem().toString();

                Dns.lookup(domain, recordType, DnsActivity.this);
            }
        });
    }

    @Override
    public void processFinish(String output) {
        this.dnsAnswer.setText(output);
    }
}
