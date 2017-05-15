package com.aaronjwood.portauthority.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private Spinner dnsRecord;
    private ProgressDialog lookupProgressDialog;
    private Handler handler;

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
        this.handler = new Handler(Looper.getMainLooper());

        this.dnsLookupClick();
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
                    return;
                }

                lookupProgressDialog = new ProgressDialog(DnsActivity.this, R.style.DialogTheme);
                lookupProgressDialog.setMessage("Querying Name Server");
                lookupProgressDialog.show();

                Object recordType = recordElement.getSelectedItem();
                if (recordType != null) {
                    String recordName = recordType.toString();
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

        handler.post(new Runnable() {

            @Override
            public void run() {
                if (lookupProgressDialog != null && lookupProgressDialog.isShowing()) {
                    lookupProgressDialog.dismiss();
                }
            }
        });
    }
}
