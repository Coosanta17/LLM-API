package net.coosanta.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.coosanta.llm.ConversationUtils.*;

public class LlamaApp {
    private final Consumer<String> responseConsumer;
    private final LlamaConfig settings = LlmController.llamaConfig;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Path savePath;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    private Conversation conversation;
    private LlamaModel model;

    private ScheduledFuture<?> deinitializeTaskFuture;


    public LlamaApp(Consumer<String> responseConsumer) throws IOException {
        this.responseConsumer = responseConsumer;

        // Load the model on startup if configured
        if (settings.getModelSettings().getLoadOnStart() && settings.getModelSettings().getInactivityTimeout() != 0) {
            loadModel();
            scheduleModelDeinitialization();
        }
    }

    public Flux<String> runConversation(UUID conversationUuid, String prompt) throws IOException {
        this.savePath = getConversationSavePathFromUuid(conversationUuid);
        this.conversation = ConversationUtils.loadFromFile(savePath);

        conversation.addMessage("User", prompt);

        InferenceParameters inferenceParameters = generateInferenceParameters(conversation);

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
                            cleanAndComplete(response);
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

    // Highly recommended to run this in a separate thread.
    public String generateTitle(Conversation conversation) {
        Conversation titleConversation = new Conversation(conversation);

        titleConversation.addMessage("User", "make a title for this conversation. Respond only with the title. Try to keep it short.");

        InferenceParameters inferenceParameters = generateInferenceParameters(titleConversation);

        String rawTitle = getModelResponse(inferenceParameters)
                .collectList()
                .map(list -> String.join("", list))
                .block(); // Block to get the result synchronously

        assert rawTitle != null; // ask ide why this is here.
        // Removes quotation marks from the title if they exist (as the model can sometimes generate them)
        return (rawTitle.startsWith("\"") && rawTitle.endsWith("\"")) ? rawTitle.substring(1, rawTitle.length() - 1) : rawTitle;
    }

    private void cleanAndComplete(StringBuilder responseBuilder) {
        String cleanedResponse = unformatMessage(responseBuilder.toString()).trim();
        conversation.addMessage("Assistant", cleanedResponse);
        try {
            ConversationUtils.saveToFile(conversation, savePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save conversation", e);
        }
    }

    private LlamaModel initializeModel(String modelPath) {
        ModelParameters modelParams = new ModelParameters()
                .setModelFilePath(modelPath)
                .setNGpuLayers(settings.getModelSettings().getGpuLayers())
                .setNCtx(settings.getModelSettings().getContext())
                .setNThreads(settings.getModelSettings().getThreads());

        return new LlamaModel(modelParams);
    }

    private static InferenceParameters generateInferenceParameters(Conversation conversation) {
        return new InferenceParameters(generateContext(conversation))
                .setTemperature(0.7f)
                .setPenalizeNl(true)
                .setMiroStat(MiroStat.V2)
                .setStopStrings("<|eot_id|>");
    }

    private static String generateContext(Conversation conversation) {
        StringBuilder generatedContext = new StringBuilder();

        // Adds system prompt to context
        generatedContext.append(conversation.getSystemPrompt());

        for (Message message : conversation.getMessages()) {
            generatedContext.append(formatMessage(message.role(), message.content()));
        }
        return generatedContext.toString();
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


