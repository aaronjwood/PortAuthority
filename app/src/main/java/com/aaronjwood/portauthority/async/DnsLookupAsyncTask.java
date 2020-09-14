package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.minidns.hla.ResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.record.Data;
import org.minidns.record.Record;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

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
        String recordType = params[1];
        ResolverResult<? extends Data> result;
        try {
            Class<Data> dataClass = Record.TYPE.valueOf(recordType).getDataClass();
            if (dataClass == null) {
                return "Record type " + recordType + " not supported";
            }

            result = ResolverApi.INSTANCE.resolve(domain, dataClass);
        } catch (IOException e) {
            return "Error performing lookup on type " + recordType + ": " + e.getMessage();
        }

        if (!result.wasSuccessful()) {
            return "Lookup of type " + recordType + " failed with response code " + result.getResponseCode();
        }

        Set<? extends Data> answers = result.getAnswers();
        if (answers.isEmpty()) {
            return "No records found for type " + recordType;
        }

        StringBuilder out = new StringBuilder();
        for (Data answer : answers) {
            out.append(answer.toString()).append("\n\n");
        }

        return out.toString();
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
