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
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.coosanta.llm.ConversationUtils.formatMessage;
import static net.coosanta.llm.ConversationUtils.unformatMessage;

public class LlamaApp {
    private final LlamaConfig settings;
    private final Consumer<String> responseConsumer;
    private final String prompt;
    private final Path savePath;

    private final Conversation conversation;

    public LlamaApp(UUID conversationUuid, String inPrompt, LlamaConfig settings, Consumer<String> responseConsumer) throws IOException {
        this.settings = settings;
        this.responseConsumer = responseConsumer;
        this.prompt = inPrompt;
        this.savePath = Path.of(settings.getConversationPath() + "/" + conversationUuid + ".json");

        conversation = ConversationUtils.loadFromFile(savePath);
    }

    public Flux<String> runConversation() throws IOException {
        LlamaModel model = initializeModel();
        conversation.addMessage("User", prompt);

        InferenceParameters inferenceParameters = new InferenceParameters(generateContext(conversation))
                .setTemperature(0.7f)
                .setPenalizeNl(true)
                .setMiroStat(MiroStat.V2)
                .setStopStrings("<|eot_id|>");

        return Flux.create(sink -> {
            try {
                getModelResponse(model, inferenceParameters)
                        .doOnNext(data -> {
                            responseConsumer.accept(data); // Log or process the response
                            sink.next(data);               // Push to API stream
                        })
                        .doOnComplete(() -> {
                            completeAndClean(new StringBuilder());
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

    private void completeAndClean(StringBuilder responseBuilder) {
        String cleanedResponse = unformatMessage(responseBuilder.toString()).trim();
        conversation.addMessage("Assistant", cleanedResponse);
        try {
            ConversationUtils.saveToFile(conversation, savePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save conversation", e);
        }
    }

    private LlamaModel initializeModel() {
        ModelParameters modelParams = new ModelParameters()
                .setModelFilePath(settings.getModelSettings().getPath())
                .setNGpuLayers(settings.getModelSettings().getGpuLayers())
                .setNCtx(settings.getModelSettings().getContext())
                .setNThreads(settings.getModelSettings().getThreads());

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

    private Flux<String> getModelResponse(LlamaModel model, InferenceParameters inferenceParams) {
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
        );
    }

}


