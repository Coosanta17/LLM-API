package net.coosanta.llm.utility;

import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaApp;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static net.coosanta.llm.utility.ConversationUtils.convertToConversation;

public class ModelUtils {
    public static void getCompletionTitleGenerated(Object conversation, SseEmitter emitter, LlamaApp llamaApp, ScheduledExecutorService scheduler) {
        try {
            Conversation completion = convertToConversation(conversation);

            assert completion != null;
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
