/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package net.frogmouth.ddf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MultiInputTransformer;


public class CsvInputTransformer implements MultiInputTransformer {

    private static final String MIME_TYPE = "text/csv";
    private MetacardTypeRegistry mTypeRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvInputTransformer.class);

    public CsvInputTransformer(MetacardTypeRegistry mTypeRegistry) {
        LOGGER.info("constructor");
        this.mTypeRegistry = mTypeRegistry;
    }

    @Override
    public List<Metacard> transform(InputStream input) throws IOException, CatalogTransformerException {
        LOGGER.info("inner transform");
        List<Metacard> metacards = new ArrayList<Metacard>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf8"));
        com.googlecode.jcsv.reader.internal.CSVReaderBuilder<String[]> builder = new com.googlecode.jcsv.reader.internal.CSVReaderBuilder<String[]>(reader);
        builder.strategy(com.googlecode.jcsv.CSVStrategy.UK_DEFAULT);
        builder.entryParser(new com.googlecode.jcsv.reader.internal.DefaultCSVEntryParser());
        com.googlecode.jcsv.reader.CSVReader<String[]> csvParser = builder.build();
        String[] header = csvParser.readNext();
        int latitudeIndex = -1;
        int longitudeIndex = -1;
        int titleIndex = -1;
        for (int i = 0; i < header.length; ++i) {
            String thisHeaderItem = header[i].trim();
            if ((thisHeaderItem.equalsIgnoreCase("lat")) || (thisHeaderItem.equalsIgnoreCase("latitude"))) {
                latitudeIndex = i;
            } else if ((thisHeaderItem.equalsIgnoreCase("lon")) || (thisHeaderItem.equalsIgnoreCase("long")) || (thisHeaderItem.equalsIgnoreCase("longitude"))) {
                longitudeIndex = i;
            } else if (titleIndex == -1) {
                // This is intended to pick the first thing that doesn't look like a lat/long attribute
                titleIndex = i;
            }
        }
        // Make sure we have both latitude and longitude
        if ((latitudeIndex == -1) || (longitudeIndex == -1)) {
            latitudeIndex = -1;
            longitudeIndex = -1;
        }
        String[] data = null;
        do {
            data = csvParser.readNext();
            if (data != null) {
                MetacardImpl mc = new MetacardImpl(BasicTypes.BASIC_METACARD);
                for (int i = 0; i < data.length; ++i) {
                    if (i == titleIndex) {
                        mc.setAttribute(Metacard.TITLE, data[i].trim());
                    }
                    // TODO: build lat and lon
                }
                mc.setAttribute(Metacard.CONTENT_TYPE, MIME_TYPE);
                metacards.add(mc);
            }
        } while (data != null); 
        LOGGER.info("leaving inner transform");
        if ((metacards == null) || (metacards.size() < 1)) {
            return null;
        } else {
            return metacards;
        }
    }
}