package com.aaronjwood.portauthority;

import java.util.ArrayList;

public interface AsyncResponse {

    void processFinish(String output);

    void processFinish(ArrayList output);
}
