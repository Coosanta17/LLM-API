/*
package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static net.coosanta.llm.LlmApiApplication.MODEL_PATH;

public class LlamaApp {

    public static void main(String[] args) {
        String systemPrompt = "You are a helpful and knowledgeable assistant.";

        // Load a previous conversation if it exists
        Path savePath = Path.of("conversation.json");
        Conversation conversation;

        if (Files.exists(savePath)) {
            try {
                conversation = ConversationUtils.loadFromFile(savePath);
                System.out.println("Loaded previous conversation.");
            } catch (IOException e) {
                e.printStackTrace();
                conversation = new Conversation(systemPrompt);
            }
        } else {
            conversation = new Conversation(systemPrompt);
        }

        // Start the interaction
        runConversation(conversation, savePath);
    }

    private static void runConversation(Conversation conversation, Path savePath) {
        Scanner scanner = new Scanner(System.in);

        // Initialize the Llama model
        ModelParameters modelParams = initializeModelParameters();
        try (LlamaModel model = new LlamaModel(modelParams)) {
            // Construct the initial prompt
            String prompt = buildPrompt(conversation);

            System.out.println("Llama: Hello. How may I help you today?");
            while (true) {
                System.out.print("User: ");
                String userInput = scanner.nextLine();

                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("Saving conversation...");
                    try {
                        ConversationUtils.saveToFile(conversation, savePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                // Add user input to the conversation
                conversation.addMessage("user", userInput);

                // Update prompt with the user input
                prompt += "\nUser: " + userInput + "\nLlama: ";

                // Generate response using the model
                InferenceParameters inferParams = new InferenceParameters(prompt)
                        .setTemperature(0.7f)
                        .setPenalizeNl(true)
                        .setMiroStat(MiroStat.V2)
                        .setStopStrings("User:");

                StringBuilder responseBuilder = new StringBuilder();
                for (LlamaOutput output : model.generate(inferParams)) {
                    System.out.print(output); // Stream response in real-time
                    responseBuilder.append(output);
                }

                String assistantResponse = responseBuilder.toString().trim();

                // Add assistant's response to the conversation
                conversation.addMessage("assistant", assistantResponse);

                // Update prompt with the assistant's response
                prompt += assistantResponse;
            }
        } finally {
            scanner.close();
        }
    }

    private static ModelParameters initializeModelParameters() {
        return new ModelParameters()
                .setModelFilePath(MODEL_PATH)
                .setNCtx(512)   // Context window size
                .setNThreads(6) // Number of threads for processing
                .setSeed(16);   // Random seed for reproducibility (debugging)
    }

    private static String buildPrompt(Conversation conversation) {
        StringBuilder builder = new StringBuilder(conversation.getSystemPrompt());
        for (Conversation.Message message : conversation.getMessages()) {
            builder.append(message.getRole().equals("user") ? "User: " : "Llama: ");
            builder.append(message.getContent()).append("\n");
        }
        return builder.toString();
    }
}
*/

package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.coosanta.llm.ConversationUtils.formatMessage;
import static net.coosanta.llm.ConversationUtils.unformatMessage;
import static net.coosanta.llm.LlmApiApplication.CONVERSATION_PATH;
import static net.coosanta.llm.LlmApiApplication.MODEL_PATH;

public class LlamaApp {

    public LlamaApp(String systemPrompt) {
        //String systemPrompt = "You are a helpful and knowledgeable assistant.";

        Path savePath = Path.of(CONVERSATION_PATH);
        Conversation conversation;

        // Load the conversation from the file if it exists
        if (Files.exists(savePath)) {
            try {
                conversation = ConversationUtils.loadFromFile(savePath);
                System.out.println("Loaded previous conversation.");
            } catch (IOException e) {
                System.out.println("Unable to read previous conversation!");
                e.printStackTrace();
                conversation = new Conversation(systemPrompt);
            }
        } else {
            conversation = new Conversation(systemPrompt);
        }

        runConversation(conversation, savePath);
    }

    private void runConversation(Conversation conversation, Path savePath) {
        Scanner scanner = new Scanner(System.in);

        ModelParameters modelParams = initializeModel();
        LlamaModel model = new LlamaModel(modelParams);

        String context = generateContext(conversation);

        while (true) {
            System.out.print("User: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) { // improve
                System.out.println("Saving conversation...");
                try {
                    ConversationUtils.saveToFile(conversation, savePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            // Adds user prompt to conversation and sends it to model
            conversation.addMessage("user", userInput);

            // Get the model's response and add it to conversation
            InferenceParameters inferenceParameters = new InferenceParameters(context + formatMessage("user", userInput))
                    .setTemperature(0.7f)
                    .setPenalizeNl(true)
                    .setMiroStat(MiroStat.V2)
                    .setStopStrings("<|eot_id|>");

            Stream<String> modelResponseStream = getModelResponse(model, inferenceParameters);

            List<String> responseList = new ArrayList<>();
            Consumer<String> livePrinter = s -> { // This shit is confusing man wtf bro
                System.out.print(s);
                responseList.add(s);
            };

            modelResponseStream.forEach(livePrinter);
            String concatenatedResponse = String.join("", responseList);

            System.out.println("Assistant: " + unformatMessage(concatenatedResponse));
            System.out.println("Assistant (debug): " + inferenceParameters);

            conversation.addMessage("assistant", unformatMessage(concatenatedResponse));
        }

        scanner.close();
    }



    // Placeholder methods for model interaction
    private ModelParameters initializeModel() {
        return new ModelParameters()
                .setModelFilePath(MODEL_PATH)
                /*.setNGpuLayers(43)*/
                .setNCtx(1024) // Make configurable
                .setNThreads(6) // Also make configurable
                .setSeed(16); // <-- Debug purposes only
    }

    private String generateContext(Conversation conversation) {
        StringBuilder generatedContext = new StringBuilder();

        // Adds system prompt to context
        generatedContext.append(conversation.getSystemPrompt());

        for (Message message : conversation.getMessages()) {
            generatedContext.append(formatMessage(message.role(), message.content()));
        }
        return generatedContext.toString();
    }

    private Stream<String> getModelResponse(LlamaModel model, InferenceParameters inferenceParams) {
        Iterator<LlamaOutput> iterator = model.generate(inferenceParams).iterator();
        Iterable<String> iterable = () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return iterator.next().toString();
            }
        };
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}


