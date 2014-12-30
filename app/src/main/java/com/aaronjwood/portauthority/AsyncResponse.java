package com.aaronjwood.portauthority;

import java.util.ArrayList;
import java.util.Map;

public interface AsyncResponse {

    void processFinish(ArrayList<Map<String, String>> output);

    void processFinish(int output);
}
