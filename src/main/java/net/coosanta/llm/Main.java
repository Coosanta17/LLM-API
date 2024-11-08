package net.coosanta.llm;

import java.io.IOException;

public class Main {
    public static final String MODEL_PATH = "path/to/your/llama-model.gguf";

    public static void main(String[] args) throws IOException {
        String yes = new Generate("Hello world!").responseMaker("Hello world!"); // placeholder, I will fix this!
    }
}
