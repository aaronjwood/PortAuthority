package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public interface Parser {

    String[] parseLine(String line);

    long saveLine(Database db, String[] data);

}
