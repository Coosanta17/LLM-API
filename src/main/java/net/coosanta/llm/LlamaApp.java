package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.coosanta.llm.ConversationUtils.formatMessage;
import static net.coosanta.llm.ConversationUtils.unformatMessage;
import static net.coosanta.llm.LlmApiApplication.CONVERSATION_PATH;

public class LlamaApp {
    private final LlamaConfig settings;

    public LlamaApp(String systemPrompt, LlamaConfig settings) {
        this.settings = settings;
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

        LlamaModel model = initializeModel();

        StringBuilder context = new StringBuilder(generateContext(conversation));

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
            context.append(formatMessage("User", userInput));

            InferenceParameters inferenceParameters = new InferenceParameters(context.toString())
                    .setTemperature(0.7f)
                    .setPenalizeNl(true)
                    .setMiroStat(MiroStat.V2)
                    .setStopStrings("<|eot_id|>");

            Stream<String> modelResponseStream = getModelResponse(model, inferenceParameters);

            StringBuilder responseBuilder = new StringBuilder();
            Consumer<String> livePrinter = s -> { // Everything done here is against best practices, but it works ok?
                System.out.print(s);
                responseBuilder.append(s);
            };

            modelResponseStream.forEach(livePrinter);
            String rawResponse = responseBuilder.toString();

            String cleanedResponse = unformatMessage(rawResponse).trim();

            conversation.addMessage("assistant", cleanedResponse);
            context.append(formatMessage("assistant", cleanedResponse));

            System.out.println("\nAssistant: " + cleanedResponse);
        }

        scanner.close();
    }



    private LlamaModel initializeModel() {
        ModelParameters modelParams = new ModelParameters()
                .setModelFilePath(settings.getModelSettings().getPath())
                .setNGpuLayers(settings.getModelSettings().getGpuLayers())
                .setNCtx(settings.getModelSettings().getContext())
                .setNThreads(settings.getModelSettings().getThreads())
                .setSeed(settings.getModelSettings().getSeed());

        return new LlamaModel(modelParams);
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


