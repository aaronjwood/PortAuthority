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
        try {
            String type = Integer.toString(Type.class.getField(recordType).getInt(null));
            new DnsLookupAsyncTask(delegate).execute(domain, type);
        } catch (NoSuchFieldException e) {
            return;
        } catch (IllegalAccessException e) {
            return;
        }
    }

}
