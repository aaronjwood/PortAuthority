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
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }

        String[] data = line.split("\\s+");
        if (!data[1].contains("tcp")) {
            return null;
        }

        String port = data[1].substring(0, data[1].indexOf("/"));
        String description = data[0];
        if (line.contains("#")) {
            description = line.substring(line.indexOf("#") + 1);
        }

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
