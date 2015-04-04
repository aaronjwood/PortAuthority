package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class GetMacInfoAsyncTask extends AsyncTask<String, Void, JSONObject> {

    private static final String TAG = "GetMacInfoAsyncTask";
    private static final String MAC_INFO_SERVICE = "http://www.macvendorlookup.com/api/v2/";
    private HostAsyncResponse delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when MAC information has been fetched
     */
    public GetMacInfoAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    /**
     * Fetch additional MAC address information
     *
     * @param params
     * @return MAC information
     */
    @Override
    protected JSONObject doInBackground(String... params) {
        String macAddress = params[0];

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(MAC_INFO_SERVICE + macAddress);
        HttpResponse response;

        try {
            response = httpclient.execute(httpget);
        }
        catch(ClientProtocolException e) {
            return null;
        }
        catch(IOException e) {
            return null;
        }

        String macInfo;
        HttpEntity entity = response.getEntity();

        try {
            return new JSONObject(EntityUtils.toString(entity));
        }
        catch(JSONException e) {
            return null;
        }
        catch(IOException e) {
            return null;
        }
    }

    /**
     * Calls the delegate when the additional MAC information has been fetched
     *
     * @param result External IP address
     */
    @Override
    protected void onPostExecute(JSONObject result) {
        delegate.processFinish(result);
    }
}
