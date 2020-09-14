package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public class PortParser implements Parser {

    /**
     * Parses the line of port data based on IANA's format.
     *
     * @param line
     * @return
     */
    @Override
    public String[] parseLine(String line) {
        String[] data = line.split(",", -1);
        if (data.length != 12) {
            return null;
        }

        String transport = data[2];
        if (!"tcp".equalsIgnoreCase(transport)) {
            return null;
        }

        String port = data[1];
        String description = data[3];

        return new String[]{port, description};
    }

    /**
     * Exports the parsed line to the database.
     *
     * @param db
     * @param data
     * @return
     */
    @Override
    public long exportLine(Database db, String[] data) {
        return db.insertPort(data[0], data[1]);
    }
}
