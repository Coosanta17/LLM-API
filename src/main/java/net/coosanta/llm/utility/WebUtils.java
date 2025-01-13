package net.coosanta.llm.utility;

import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaApp;
import net.coosanta.llm.LlamaConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.coosanta.llm.LlmController.llamaConfig;
import static net.coosanta.llm.utility.ConversationUtils.*;

public class WebUtils {

    public static void ping(ScheduledExecutorService scheduler, SseEmitter emitter) {
        if (llamaConfig.isSsePing()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event().name("ping"));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }, llamaConfig.getPingInterval(), llamaConfig.getPingInterval(), TimeUnit.SECONDS);
        }
    }

    public static @NotNull Runnable closeChatStream(
            ScheduledExecutorService scheduler,
            Conversation conversation,
            Disposable disposable,
            StringBuffer modelResponse,
            LlamaApp llamaApp) {

        return () -> {
            scheduler.close();
            disposable.dispose();
            if (llamaConfig.isSaveCompletionConversations() && conversation != null) {
                saveCompletionConversation(conversation, modelResponse, llamaApp);
            }
        };
    }

    private static void saveCompletionConversation(Conversation conversation, StringBuffer modelResponse, LlamaApp llamaApp) {
        conversation.addMessage("Assistant", modelResponse.toString(), null);
        LlamaConfig.ModelConfig modelConfig = llamaConfig.getModelSettings();
        try {
            if (llamaApp.getModel() == null) {
                System.err.println("Cannot calculate token length - model is unloaded.");
            } else {
                int totalTokenLength = llamaApp.calculateTokenLength(generateContext(conversation));
                System.out.println("Total toke length: "+totalTokenLength); //debug

                conversation.setTotalTokenLength(totalTokenLength);
                System.out.println("Set totalTokenLength variable in conversation");

                int contextPerSlot = modelConfig.getContext() / modelConfig.getParallelSequences();
                System.out.println("Calculated context per slot");

                int tokenLengthAtLastSystemPrompt = conversation.getTokenLengthAtLastSystemPrompt() == null ? 0 : conversation.getTokenLengthAtLastSystemPrompt();

                int tokenLengthSinceLastSystemPrompt = totalTokenLength - tokenLengthAtLastSystemPrompt;
                System.out.println("Calculated the token system prompt thing");

                //debug
                System.out.println("Context per slot: " + contextPerSlot);
                System.out.println("Token length since last system prompt: " + tokenLengthSinceLastSystemPrompt);

                if (tokenLengthSinceLastSystemPrompt >= contextPerSlot - contextPerSlot * 0.1) {
                    conversation.addMessage("System", conversation.getSystemPrompt(), null);
                    conversation.setTokenLengthAtLastSystemPrompt(totalTokenLength);
                    System.out.println("Injected System prompt to keep model on track");//debug
                } else {
                    System.out.println("No need to inject system prompt");//debug
                }
            }
            saveToFile(conversation, getConversationSavePathFromUuid(conversation.getUuid()));
        } catch (IOException e) {
            System.err.println("Failed to save conversation");
            throw new RuntimeException(e);
        }
    }

    public static void streamResponse(String data, SseEmitter emitter, StringBuffer modelResponseString) {
        try {
            emitter.send(SseEmitter.event().data(data));
            modelResponseString.append(data);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    public static void getCompletionTitleGenerated(
            Object conversation,
            SseEmitter emitter,
            LlamaApp llamaApp,
            ScheduledExecutorService scheduler) {

        try {
            Conversation completion = convertToConversation(conversation);

            if (completion.getMessages().isEmpty()) {
                throw new IllegalArgumentException("Cannot generate title, conversation is empty!");
            }

            CompletableFuture<String> futureTitle = CompletableFuture.supplyAsync(() -> llamaApp.generateTitle(completion));

            futureTitle.thenAccept(generatedTitle -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("title").data(generatedTitle));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                } finally {
                    scheduler.close();
                }
            });
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
