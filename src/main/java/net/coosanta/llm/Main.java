package net.coosanta.llm;

import java.io.IOException;

public class Main {
    public static final String MODEL_PATH = "path/to/your/llama-model.gguf";

    public static void main(String[] args) throws IOException {

        // example config generation and reading.
        ConfigManager configuration = new ConfigManager();
        System.out.println("App Name: " + configuration.getProperty("app.name"));
        configuration.setProperty("app.name", "UpdatedJavaApp");
        System.out.println("Updated App Name: " + configuration.getProperty("app.name"));
        System.out.println("App Version: " + configuration.getProperty("app.version"));
    }
}
