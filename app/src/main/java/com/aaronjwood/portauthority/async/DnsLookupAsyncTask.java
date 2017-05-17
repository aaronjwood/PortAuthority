package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import java.lang.ref.WeakReference;

public class DnsLookupAsyncTask extends AsyncTask<String, Void, String> {

    private final WeakReference<DnsAsyncResponse> delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when the DNS lookup finishes
     */
    public DnsLookupAsyncTask(DnsAsyncResponse delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    /**
     * Performs the appropriate lookup for specified record type
     *
     * @param params
     * @return DNS answer
     */
    @Override
    protected String doInBackground(String... params) {
        String domain = params[0];
        int recordType = Integer.parseInt(params[1]);
        Record[] records;

        try {
            records = new Lookup(domain, recordType).run();
            if (records == null) {
                return "No records found.";
            }

            String answer = "";

            for (Record record : records) {
                String rClass = this.parseRecordClass(record.getDClass());
                answer += String.format("%s\t\t\t\t%s\t\t\t\t%s\t\t\t\t%s%n%n", record.getName(), record.getTTL(), rClass, record.rdataToString());
            }

            return answer;
        } catch (TextParseException e) {
            return "Error performing lookup!";
        }
    }

    /**
     * Determines the string representation of the DNS record class
     *
     * @param recordClass Numeric record class
     * @return Human readable record class
     */
    private String parseRecordClass(int recordClass) {
        switch (recordClass) {
            case 1:
                return "IN";
            case 2:
                return "CS";
            case 3:
                return "CH";
            case 4:
                return "HS";
            default:
                return "IN";
        }
    }

    /**
     * Calls the delegate when the DNS lookup has finished
     *
     * @param result DNS answer
     */
    @Override
    protected void onPostExecute(String result) {
        DnsAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(result);
        }
    }
}
