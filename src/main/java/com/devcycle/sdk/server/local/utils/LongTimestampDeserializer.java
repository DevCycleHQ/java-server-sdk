package com.devcycle.sdk.server.local.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class LongTimestampDeserializer extends StdDeserializer<Long> {
    public LongTimestampDeserializer() {
        super(Long.class);
    }

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException {
        String timestamp = parser.getText();
        try {
            return Instant.parse(timestamp).toEpochMilli();
        } catch (DateTimeParseException dtpe) {
            throw new InvalidFormatException(
                    parser, dtpe.getMessage(), timestamp, Long.class);
        }
    }
}