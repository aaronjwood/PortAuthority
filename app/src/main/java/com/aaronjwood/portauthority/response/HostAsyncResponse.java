package com.aaronjwood.portauthority.response;

import java.util.ArrayList;

public interface HostAsyncResponse {

    void processFinish(ArrayList<Integer> output);

    void processFinish(int output);

    void processFinish(String output);
}
