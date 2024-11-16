package net.coosanta.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import static net.coosanta.llm.LlmApiApplication.MODEL_PATH;

@SuppressWarnings("InfiniteLoopStatement")
public class Responder {

    public Response respond() throws IOException {
        ModelParameters modelParams = new ModelParameters()
                .setModelFilePath(MODEL_PATH)
                /*.setNGpuLayers(43)*/
                .setNCtx(512) // Make configurable
                .setNThreads(6) // Also make configurable
                .setSeed(16); // <-- Debug purposes only


        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try (LlamaModel model = new LlamaModel(modelParams)) {
            ChatContext chatContext = new ChatContext();

            chatContext.addMessage("System",
                    """
                            This is a conversation between User and Llama, a friendly chatbot.
                            Llama is helpful, kind, honest, good at writing, and never fails to answer any \
                            requests immediately and with precision.
                            If Llama doesn't know something they will be honest about it
                            
                            """);

            chatContext.addMessage("User", "Hello Llama");
            chatContext.addMessage("Llama", "Hello. How may I help you today?");

            while (true) {
                System.out.print("\nUser: ");
                String input = reader.readLine();
                chatContext.addMessage("User", input);

                InferenceParameters inferParams = new InferenceParameters(chatContext.getContextAsString())
                        .setTemperature(0.7f)
                        .setPenalizeNl(true)
                        .setMiroStat(MiroStat.V2)
                        .setStopStrings("User:");

                System.out.print("Llama: ");
                for (LlamaOutput output : model.generate(inferParams)) {
                    System.out.print(output);
                    chatContext.addMessage("Llama", output.toString());
                }
            }

        }
    }
}