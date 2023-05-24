package com.devcycle.sdk.server.helpers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TestDataFixtures {
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


    public static String SmallConfig() {
        return loadConfigData("fixture_small_config.json");
    }

    public static String LargeConfig() {
        return loadConfigData("fixture_large_config.json");
    }

    public static String SmallConfigWithSpecialCharacters() {
        return loadConfigData("fixture_small_config_special_characters.json");
    }

    public static String SmallConfigWithCustomDataBucketing() {
        return loadConfigData("fixture_small_config_custom_data_bucketing.json");
    }
}
