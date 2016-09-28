package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.DnsLookupAsyncTask;
import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.xbill.DNS.Type;

public final class Dns {

    /**
     * Kicks off a DNS lookup
     *
     * @param domain     Domain name
     * @param recordType The type of DNS record to look up
     * @param delegate   Delegate to be called when the lookup has finished
     */
    public static void lookup(String domain, String recordType, DnsAsyncResponse delegate) {
        String type;

        switch (recordType) {
            case "A":
                type = Integer.toString(Type.A);
                break;
            default:
                return;
        }

        new DnsLookupAsyncTask(delegate).execute(domain, type);
    }

}
