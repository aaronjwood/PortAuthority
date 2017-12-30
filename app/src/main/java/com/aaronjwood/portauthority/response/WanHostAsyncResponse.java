package com.aaronjwood.portauthority.response;

import android.util.SparseArray;

interface WanHostAsyncResponse {

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
    void processFinish(SparseArray<String> output);
}
