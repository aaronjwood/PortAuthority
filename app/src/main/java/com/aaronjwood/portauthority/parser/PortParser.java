package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public class PortParser implements Parser {

    /**
     * Parses the line of the port data CSV file provided by IANA.
     *
     * @param line
     * @return
     */
    @Override
    public String[] parseLine(String line) {
        if (line.isEmpty() || line.startsWith(" ") || line.startsWith(",")) {
            return null;
        }

        String[] data = line.split(",");
        if (data.length < 3 || !data[2].contains("tcp")) {
            return null;
        }

        String port = data[1];
        if (!port.matches("\\d+")){
            return null;
        }
        String description = data.length >= 4 ? data[3] : data[0];

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
