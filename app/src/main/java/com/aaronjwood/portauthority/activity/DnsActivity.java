package com.aaronjwood.portauthority.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.async.DnsLookupAsyncTask;
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

        this.domainName = findViewById(R.id.domainName);
        this.dnsAnswer = findViewById(R.id.dnsAnswer);
        this.dnsRecord = findViewById(R.id.recordSpinner);
        this.domainName.setText(UserPreference.getLastUsedDomainName(this));
        this.dnsRecord.setSelection(UserPreference.getLastUsedDnsRecord(this));

        this.dnsLookupClick();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedState) {
        super.onSaveInstanceState(savedState);

        String recordData = dnsAnswer.getText().toString();
        savedState.putString("records", recordData);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
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
        final EditText domainElement = findViewById(R.id.domainName);
        final Spinner recordElement = findViewById(R.id.recordSpinner);
        Button dnsLookupButton = findViewById(R.id.dnsLookup);
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
                    new DnsLookupAsyncTask(DnsActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, domain, recordName);
                }
            }
        });
    }

    /**
     * Displays the DNS answer(s) to the user
     *
     * @param output DNS record data
     */
    @Override
    public void processFinish(String output) {
        this.dnsAnswer.setText(output);
    }
}
