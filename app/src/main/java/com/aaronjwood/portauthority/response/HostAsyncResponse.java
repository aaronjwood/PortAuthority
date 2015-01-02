package com.aaronjwood.portauthority.response;

public interface HostAsyncResponse {

    void processFinish(int output);

    void processFinish(boolean output);

    void processFinish(String output);
}
