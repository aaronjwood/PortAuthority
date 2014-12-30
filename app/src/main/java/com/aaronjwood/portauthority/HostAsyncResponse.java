package com.aaronjwood.portauthority;

import java.util.ArrayList;

public interface HostAsyncResponse {

    void processFinish(ArrayList<Integer> output);

    void processFinish(int output);
}
