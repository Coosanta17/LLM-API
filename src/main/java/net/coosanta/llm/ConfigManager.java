package net.coosanta.llm;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "./config.properties";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    // Load configuration from file or create default if it doesn't exist
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
                System.out.println("Configuration loaded from " + CONFIG_FILE);
            } catch (IOException ex) {
                System.err.println("Error reading configuration file: " + ex.getMessage());
            }
        } else {
            System.out.println("Configuration file not found. Creating default configuration...");
            createDefaultConfig();
            saveConfig();
        }
    }

    private void createDefaultConfig() {
        properties.setProperty("app.name", "Example");
        properties.setProperty("app.version", "0.1");
        properties.setProperty("logging.level", "INFO");
        properties.setProperty("max.threads", "12400");
    }

    private void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Application Configuration");
            System.out.println("Default configuration saved to " + CONFIG_FILE);
        } catch (IOException ex) {
            System.err.println("Error saving configuration file: " + ex.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }
}
