package net.coosanta.llm.utility;

import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaApp;
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
            StringBuffer modelResponse) {

        return () -> {
            scheduler.close();
            disposable.dispose();
            if (llamaConfig.isSaveCompletionConversations() && conversation != null) {
                saveCompletionConversation(conversation, modelResponse);
            }
        };
    }

    private static void saveCompletionConversation(Conversation conversation, StringBuffer modelResponse) {
        conversation.addMessage("Assistant", modelResponse.toString(), null);
        try {
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
