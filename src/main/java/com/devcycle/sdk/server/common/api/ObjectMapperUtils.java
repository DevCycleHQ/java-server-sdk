package com.devcycle.sdk.server.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for providing pre-configured ObjectMapper instances
 * with consistent settings across the DevCycle SDK.
 */
public class ObjectMapperUtils {

    /**
     * Creates a new ObjectMapper with DevCycle SDK default configuration:
     * - Ignores unknown properties during deserialization
     * - Excludes null values from serialization
     * - Uses consistent date/time formatting
     * 
     * @return A pre-configured ObjectMapper instance
     */
    public static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Ignore unknown properties to handle API changes gracefully
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Don't include null values in JSON output
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        return mapper;
    }

    /**
     * Creates an ObjectMapper specifically configured for event processing
     * with additional date formatting settings.
     * 
     * @return A pre-configured ObjectMapper for events
     */
    public static ObjectMapper createEventObjectMapper() {
        ObjectMapper mapper = createDefaultObjectMapper();
        
        // Disable timestamp serialization for events
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
} 