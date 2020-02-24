package com.aaronjwood.portauthority.async;

import android.content.Context;
import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.config.AndroidResolverConfigProvider;
import org.xbill.DNS.config.InitializationException;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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
        AndroidResolverConfigProvider resolverCfg = new AndroidResolverConfigProvider();
        Context ctx = (Context) this.delegate.get();
        AndroidResolverConfigProvider.setContext(ctx);
        Record[] records;
        try {
            resolverCfg.initialize();
            String[] servers = new String[resolverCfg.servers().size()];
            for (int i = 0; i < resolverCfg.servers().size(); i++) {
                InetSocketAddress server = resolverCfg.servers().get(i);
                servers[i] = server.getHostName();
            }

            Resolver resolver = new ExtendedResolver(servers);
            Lookup lookup = new Lookup(domain, recordType);
            lookup.setResolver(resolver);
            records = lookup.run();
            if (records == null || records.length == 0) {
                return "No records found.";
            }

            StringBuilder answer = new StringBuilder();
            for (Record record : records) {
                String rClass = this.parseRecordClass(record.getDClass());
                answer.append(String.format("%s\t\t\t\t%s\t\t\t\t%s\t\t\t\t%s%n%n", record.getName(), record.getTTL(), rClass, record.rdataToString()));
            }

            return answer.toString();
        } catch (TextParseException e) {
            return "Error performing lookup: " + e.getMessage();
        } catch (InitializationException e) {
            return "Error initializing resolver: " + e.getMessage();
        } catch (UnknownHostException e) {
            return "Resolver host is unknown:: " + e.getMessage();
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
