package com.aaronjwood.portauthority.response;

import org.json.JSONObject;

import java.util.Map;

public interface HostAsyncResponse {

    /**
     * Delegate to handle integer outputs
     *
     * @param output
     */
    void processFinish(int output);

    /**
     * Delegate to handle boolean outputs
     *
     * @param output
     */
    void processFinish(boolean output);

    /**
     * Delegate to handle Map outputs
     *
     * @param output
     */
    void processFinish(Map<Integer, String> output);

    /**
     * Delegate to handle string outputs
     *
     * @param output
     */
    void processFinish(String output);

    /**
     * Delegate to handle JSONObject outputs
     *
     * @param output
     */
    void processFinish(JSONObject output);
}
