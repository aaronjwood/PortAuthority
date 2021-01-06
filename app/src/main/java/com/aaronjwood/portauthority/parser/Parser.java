package com.aaronjwood.portauthority.parser;

import com.aaronjwood.portauthority.db.Database;

public interface Parser {

    /**
     * Parses a line of data from a foreign source.
     *
     * @param line
     * @return
     */
    String[] parseLine(String line);

    /**
     * Exports a parsed line of data to the database.
     *
     * @param db
     * @param data
     * @return
     */
    long exportLine(Database db, String[] data);

}
