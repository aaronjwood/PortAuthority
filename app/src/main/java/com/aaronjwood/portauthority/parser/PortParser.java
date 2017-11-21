package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public class PortParser implements Parser {

    @Override
    public String[] parseLine(String line) {
        String[] data = line.split(",");
        if (data.length != 11) {
            return null;
        }

        String transport = data[2];
        if (!"tcp".equalsIgnoreCase(transport)) {
            return null;
        }

        String port = data[1];
        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return null;
        }

        String description = data[3];

        return new String[]{port, description};
    }

    @Override
    public long saveLine(Database db, String[] data) {
        int port = Integer.parseInt(data[0]);
        return db.insertPort(port, data[1]);
    }
}
