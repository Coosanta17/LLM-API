package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
