package net.coosanta.llm;

import de.kherud.llama.ModelParameters;

import java.io.IOException;

import static net.coosanta.llm.Main.MODEL_PATH;

public class Generate {
    private final ModelParameters modelParameters = new ModelParameters()
            .setModelFilePath(MODEL_PATH);

    public Generate(String prompt) throws IOException {
        String response = responseMaker(prompt);
    }

    public String responseMaker(String prompt) throws IOException {
        // code here
        return prompt;
    }
}
