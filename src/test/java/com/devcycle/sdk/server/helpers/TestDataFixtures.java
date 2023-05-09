package com.devcycle.sdk.server.helpers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TestDataFixtures {
    public static final String SMALL_CONFIG = loadConfigData("fixture_small_config.json");
    public static final String LARGE_CONFIG = loadConfigData("fixture_large_config.json");
    public static final String SPECIAL_CHARACTERS_CONFIG = loadConfigData("fixture_small_config_special_characters.json");

    /**
     * Loads the config data from the file in the resources folder
     * @param fileName name of the file to load
     * @return the String contents of that resource file
     */
    private static String loadConfigData(String fileName)  {
        String configData = "";
        ClassLoader classLoader = TestDataFixtures.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                configData += line;
            }
        } catch (IOException e) {
            System.out.println("Failed to load config data ["+fileName+"]: " + e.getMessage());
            e.printStackTrace();
        }
        return configData;
    }
}
