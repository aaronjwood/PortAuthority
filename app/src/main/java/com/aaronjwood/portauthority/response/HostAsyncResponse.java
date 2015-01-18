package com.aaronjwood.portauthority.response;

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
     * Delegate to handle string outputs
     *
     * @param output
     */
    void processFinish(String output);
}
