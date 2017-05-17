package com.aaronjwood.portauthority.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Dns;
import com.aaronjwood.portauthority.response.DnsAsyncResponse;
import com.aaronjwood.portauthority.utils.UserPreference;

public final class DnsActivity extends AppCompatActivity implements DnsAsyncResponse {

    private EditText domainName;
    private TextView dnsAnswer;
    private Spinner dnsRecord;

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
        this.dnsRecord = (Spinner) findViewById(R.id.recordSpinner);
        this.domainName.setText(UserPreference.getLastUsedDomainName(this));
        this.dnsRecord.setSelection(UserPreference.getLastUsedDnsRecord(this));

        this.dnsLookupClick();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        String recordData = dnsAnswer.getText().toString();
        savedState.putString("records", recordData);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String recordData = savedInstanceState.getString("records");
        dnsAnswer.setText(recordData);
    }

    /**
     * Clean up
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserPreference.saveLastUsedDomainName(this, this.domainName.getText().toString());
        UserPreference.saveLastUsedDnsRecord(this, this.dnsRecord.getSelectedItemPosition());
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
                if (domain.isEmpty() || recordElement.getSelectedItemPosition() == 0) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.dnsInputError), Toast.LENGTH_LONG).show();
                    return;
                }

                Object recordType = recordElement.getSelectedItem();
                if (recordType != null) {
                    String recordName = recordType.toString();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.startingDnsLookup), Toast.LENGTH_SHORT).show();
                    Dns.lookup(domain, recordName, DnsActivity.this);
                }
            }
        });
    }

    /**
     * Displays the DNS answers to the user
     *
     * @param output
     */
    @Override
    public void processFinish(String output) {
        this.dnsAnswer.setText(output);

    }
}
