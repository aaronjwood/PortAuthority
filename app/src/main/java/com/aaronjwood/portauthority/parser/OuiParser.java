package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public class OuiParser implements Parser {

    @Override
    public String[] parseLine(String line) {
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }

        String[] data = line.split("\\t");
        String mac = data[0].toLowerCase();
        String vendor;
        if (data.length == 3) {
            vendor = data[2];
        } else {
            vendor = data[1];
        }

        return new String[]{mac, vendor};

    }

    @Override
    public long saveLine(Database db, String[] line) {
        return db.insertOui(line[0], line[1]);
    }

}
