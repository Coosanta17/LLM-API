package net.coosanta.llm;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import static net.coosanta.llm.utility.ConversationUtils.*;
import static net.coosanta.llm.utility.WebUtils.*;

// Ignore IDE warnings about these classes, they are used and are quite important!
@CrossOrigin(origins = "*", allowedHeaders = "*") // Other methods to allow Cors didn't work so I am covering eveything for now.
@RestController
@RequestMapping("/api/v1/")
public class LlmController {
    public static LlamaConfig llamaConfig;
    private final LlamaApp llamaApp;

    public LlmController(LlamaConfig llamaConfig) {
        try {
            LlmController.llamaConfig = llamaConfig;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize LlmController due to configuration error", e);
        }
        try {
            this.llamaApp = new LlamaApp(System.out::print);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize LlamaApp", e);
        }
    }

    // curl -X GET http://localhost:8080/api/v1/check
    @GetMapping("/check")
    public ResponseEntity<Void> checkOnline() {
        return ResponseEntity.ok().build();
    }

    // Bash (String): curl -X POST -H "Content-Type: application/json" -d '"your-string-input-here"' "http://localhost:8080/api/v1/complete?type=string"
    // Bash (Conversation): curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"your-system-prompt","messages":[{"role":"User","content":"your-message"}, {...}, {...}]}' "http://localhost:8080/api/v1/complete?type=conversation"
    // Bash (Also string): curl -X POST -H "Content-Type: application/json" -d '"your-string-input-here"' "http://localhost:8080/api/v1/complete"
    @PostMapping(value = "/complete", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completeChat(@RequestParam(required = false) String type, @RequestBody Object input) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        StringBuffer modelResponse = new StringBuffer();

        try {
            emitter.send(SseEmitter.event()
                    .name("generating"));

            Flux<String> response;

            Conversation conversation = null;
            if (type == null || Objects.equals(type.toLowerCase(), "string")) {
                response = llamaApp.completeString((String) input);
            } else if (Objects.equals(type.toLowerCase(), "conversation")) {
                conversation = new Conversation(convertToConversation(input));

                response = llamaApp.completeConversation(conversation);
            } else {
                emitter.completeWithError(new IllegalArgumentException("Invalid type: " + type));
                return emitter;
            }

            ping(scheduler, emitter);

            // Send response stream
            Disposable subscription = response.subscribe(
                    data -> streamResponse(data, emitter, modelResponse),
                    emitter::completeWithError,
                    emitter::complete
            );

            emitter.onCompletion(closeChatStream(scheduler, conversation, subscription, modelResponse));

            emitter.onTimeout(() -> {
                if (!subscription.isDisposed()) {
                    emitter.completeWithError(new TimeoutException("Request timed out"));
                } else {
                    System.out.println("Emitter failed to close after subscription was disposed");
                    emitter.complete();
                }
            });

        } catch (Exception e) {
            emitter.completeWithError(e);
            scheduler.shutdown();
        }

        return emitter;
    }

    @PostMapping("/complete-string-no-stream")
    public CompletableFuture<ResponseEntity<String>> completeStringNoStream(@RequestBody String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = llamaApp.noStreamInference(prompt);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.err.println("Error during inference: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to complete inference", e);
            }
        });
    }

    // Bash: curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"your-system-prompt","messages":[{"role":"User","content":"your-message"}, {"role":"Assistant","content":"response-message"}]}' "http://localhost:8080/api/v1/completion-title"
    @PostMapping("/completion-title")
    public SseEmitter setTitleCompletion(@RequestBody Object conversation) {
        SseEmitter emitter = new SseEmitter(0L);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            emitter.send(SseEmitter.event().name("generating"));

            CompletableFuture.runAsync(() -> getCompletionTitleGenerated(conversation, emitter, llamaApp, scheduler));

            ping(scheduler, emitter);

        } catch (Exception e) {
            emitter.completeWithError(e);
            scheduler.close();
        }

        return emitter;
    }
}