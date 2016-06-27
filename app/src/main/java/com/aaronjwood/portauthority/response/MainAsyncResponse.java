package com.aaronjwood.portauthority.response;

import java.util.Map;

public interface MainAsyncResponse {

    /**
     * Delegate to handle map outputs
     *
     * @param output
     */
    void processFinish(Map<String, String> output);

    /**
     * Delegate to handle integer outputs
     *
     * @param output
     */
    void processFinish(int output);

    /**
     * Delegate to handle string outputs
     *
     * @param output
     */
    void processFinish(String output);
}
