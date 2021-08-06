package com.leantech.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.leantech.server.utils.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConfigurationManager {
    private static ConfigurationManager theConfigurationManager;
    private static Configuration currentConfiguration;

    private ConfigurationManager(){}

    public static ConfigurationManager getInstance(){
        if(theConfigurationManager == null)
            theConfigurationManager = new ConfigurationManager();
        return theConfigurationManager;
    }

    /**
     * Loads the configuration file located in the path provided
     * @param filepath
     */
    public void loadConfigurationFile(String filepath) {
        try {
            FileReader fileReader = new FileReader(filepath);
            StringBuffer stringBuffer = new StringBuffer();
            int i;
            while ((i = fileReader.read()) != -1) {
                stringBuffer.append((char) i);
            }

            JsonNode configuration = JsonReader.parse(stringBuffer.toString());
            currentConfiguration = JsonReader.fromJson(configuration, Configuration.class);
        } catch (FileNotFoundException exception){
            System.out.println("Configuration file not found in: " + filepath);
        } catch (IOException exception){
            System.out.println("Error while reading the configuration file");
        }
    }

    /**
     * Returns the current loaded configuration
     */
    public Configuration getCurrentConfiguration(){
        if(currentConfiguration == null)
            System.out.println("There is no configuration set");
        return currentConfiguration;
    }
}
