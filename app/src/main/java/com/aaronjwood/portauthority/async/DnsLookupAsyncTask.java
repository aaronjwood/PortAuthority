package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import java.lang.ref.WeakReference;
import java.util.List;

public class DnsLookupAsyncTask extends AsyncTask<List<String>, Void, String> {

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
    protected String doInBackground(List<String>... params) {
        String domain = params[0].get(0);
        int recordType = Integer.parseInt(params[0].get(1));
        Record[] records;

        try {
            records = new Lookup(domain, recordType).run();
            String answer = "";

            for (Record record : records) {
                answer += record.rdataToString();
            }

            return answer;
        } catch (TextParseException e) {
            return "Could not lookup record!";
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
