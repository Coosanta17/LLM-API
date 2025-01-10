package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.coosanta.llm.ConversationUtils.*;

public class LlamaApp {
    private LlamaModel model;

    private final Consumer<String> responseConsumer;
    private final LlamaConfig settings = LlmController.llamaConfig;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final LinkedHashMap<UUID, Conversation> loadedConversations = new LinkedHashMap<>();
    private int conversationsLoaded = 0;

    private final HashMap<UUID, StringBuilder> loadedConversationContext = new LinkedHashMap<>();

    private ScheduledFuture<?> deinitializeTaskFuture;


    public LlamaApp(Consumer<String> responseConsumer) {
        this.responseConsumer = responseConsumer;

        // Load the model on startup if configured
        if (settings.getModelSettings().getLoadOnStart() && settings.getModelSettings().getInactivityTimeout() != 0) {
            loadModel();
            scheduleModelDeinitialization();
        }

        if (settings.getLoadedConversationsLimit() < 1) {
            throw new IllegalArgumentException("Loaded conversation limit must be greater than 0");
        }
    }

    public Flux<String> runConversation(UUID conversationUuid, String prompt) throws IOException {
        Path savePath = getConversationSavePathFromUuid(conversationUuid);

        // Loads the conversation if it hasn't been loaded yet
        if (!loadedConversations.containsKey(conversationUuid)) {
            Conversation loadedConversation = ConversationUtils.loadFromFile(savePath);

            String generatedContext = generateContext(loadedConversation);
            loadedConversation.setTotalTokenLength(calculateTokenLength(generatedContext));

            loadedConversations.put(conversationUuid, loadedConversation);

            loadedConversationContext.put(conversationUuid, new StringBuilder(generatedContext));

            conversationsLoaded++;
        } else {
            loadedConversationContext.get(conversationUuid).append(prompt);
        }

        if (conversationsLoaded >= settings.getLoadedConversationsLimit()) {
            UUID uuidOfConversationThatWillSoonGetUnloaded = loadedConversations.firstEntry().getKey();

            loadedConversations.remove(uuidOfConversationThatWillSoonGetUnloaded);
            loadedConversationContext.remove(uuidOfConversationThatWillSoonGetUnloaded);

            conversationsLoaded--;
        }

        Conversation conversation = loadedConversations.get(conversationUuid);
        String context = String.valueOf(loadedConversationContext.get(conversationUuid));

        // IDK how to make this not bad.
        conversation.addMessage("User", prompt, calculateTokenLength(formatMessage("User", prompt)));

        InferenceParameters inferenceParameters = generateStringInferenceParameters(context);

        StringBuilder response = new StringBuilder();

        return Flux.create(sink -> {
            try {
                getModelResponse(inferenceParameters)
                        .doOnNext(data -> {
                            responseConsumer.accept(data); // Log the response (debug)
                            sink.next(data);               // Push to API stream
                            response.append(data);         // Append to response

                        })
                        .doOnComplete(() -> {
                            cleanCompleteAndSave(response, savePath, conversation);
                            loadedConversationContext.get(conversationUuid).append(response);
                            sink.complete();
                        })
                        .doOnError(sink::error) // Forward errors
                        .subscribeOn(Schedulers.boundedElastic()) // Run in a new thread
                        .subscribe();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Flux<String> completeConversation(Conversation input) {
        InferenceParameters incomplete = generateInferenceParameters(input);

        return getCompletionFlux(incomplete);
    }

    public Flux<String> completeString(String input) {
        InferenceParameters incomplete = generateStringInferenceParameters(input);

        return getCompletionFlux(incomplete);
    }

    private @NotNull Flux<String> getCompletionFlux(InferenceParameters uncompleted) {
        return Flux.create(sink -> {
            try {
                getModelResponse(uncompleted)
                        .doOnNext(data -> {
                            responseConsumer.accept(data);
                            sink.next(data);
                        })
                        .doOnComplete(sink::complete)
                        .doOnError(sink::error) // Forward errors
                        .subscribeOn(Schedulers.boundedElastic()) // Run in a new thread
                        .subscribe();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    // Highly recommended to run this in a separate thread.
    public String generateTitle(Conversation conversation) {
        Conversation titleConversation = new Conversation(conversation);

        titleConversation.setSystemPrompt("");

        titleConversation.addMessage("User", "make a title for this conversation. Respond only with the title. Try to keep it short.", null);

        InferenceParameters inferenceParameters = generateInferenceParameters(titleConversation);

        String rawTitle = getModelResponse(inferenceParameters)
                .collectList()
                .map(list -> String.join("", list))
                .block(); // Block to get the result synchronously

        assert rawTitle != null; // ask ide why this is here.
        // Removes quotation marks from the title if they exist (as the model can sometimes generate them)
        return (rawTitle.startsWith("\"") && rawTitle.endsWith("\"")) ? rawTitle.substring(1, rawTitle.length() - 1) : rawTitle;
    }

    // How to make this not bound to LlamaApp class??
    public int calculateTokenLength(String formattedInput) {
        if (!isModelLoaded()) {
            return -1; // Hehe
        }
        return model.encode(formattedInput).length;
    }

    private boolean isModelLoaded() {
        if (model == null) {
            System.out.println("No loaded model.");
            return false;
        }
        return true;
    }

    private void cleanCompleteAndSave(StringBuilder responseBuilder, Path savePath, Conversation conversation) {
        String cleanedResponse = unformatMessage(responseBuilder.toString()).trim();

        // Fix this
        conversation.addMessage("Assistant", cleanedResponse, calculateTokenLength(formatMessage("Assistant", cleanedResponse)));
        try {
            ConversationUtils.saveToFile(conversation, savePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save conversation", e);
        }
    }

    private LlamaModel initializeModel(String modelPath) {
        final LlamaConfig.ModelConfig modelSettings = settings.getModelSettings();
        ModelParameters modelParams = new ModelParameters()
                .setModelFilePath(modelPath)
                .setNGpuLayers(modelSettings.getGpuLayers())
                .setNCtx(modelSettings.getContext())
                .setNThreads(modelSettings.getThreads())
                .setNPredict(modelSettings.getResponseLimit());

        return new LlamaModel(modelParams);
    }

    private static InferenceParameters generateInferenceParameters(Conversation conversation) {
        return new InferenceParameters(generateContext(conversation))
                .setTemperature(0.7f)
                .setPenalizeNl(true)
                .setMiroStat(MiroStat.V2)
                .setStopStrings("<|eot_id|>");
    }

    private static InferenceParameters generateStringInferenceParameters(String input) {
        return new InferenceParameters(input)
                .setTemperature(0.7f)
                .setPenalizeNl(true)
                .setMiroStat(MiroStat.V2)
                .setStopStrings("<|eot_id|>");
    }

    public void setIgnoreMessage(Conversation conversation, int messageIndex, boolean ignored) throws IOException {
        conversation.setMessageIgnore(messageIndex, ignored);
        ConversationUtils.saveToFile(conversation, getConversationSavePathFromUuid(conversation.getUuid()));
    }

    private Flux<String> getModelResponse(InferenceParameters inferenceParams) {
        loadModel();

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

        return Flux.using(
                        () -> StreamSupport.stream(iterable.spliterator(), false),
                        Flux::fromStream,
                        Stream::close
                )
                .doOnComplete(this::scheduleModelDeinitialization);
    }

    private void scheduleModelDeinitialization() {
        int inactivityTimeout = settings.getModelSettings().getInactivityTimeout();

        if (inactivityTimeout == -1) {
            return; // Never unload the model
        }

        Runnable deinitializeTask = () -> {
            model.close();
            model = null;
            System.out.println("Unloaded model");
        };

        if (inactivityTimeout == 0) {
            deinitializeTask.run(); // Unload the model immediately after generating
        } else {
            // Schedule model deinitialization after configured minutes of inactivity
            deinitializeTaskFuture = scheduler.schedule(deinitializeTask, inactivityTimeout, TimeUnit.MINUTES);
        }
    }

    private void loadModel() {
        if (model == null) {
            model = initializeModel(settings.getModelSettings().getPath());
        }

        if (deinitializeTaskFuture != null) {
            deinitializeTaskFuture.cancel(false);
        }
    }

}


