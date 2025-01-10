package net.coosanta.llm;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static net.coosanta.llm.utility.ConversationUtils.*;
import static net.coosanta.llm.utility.ModelUtils.getCompletionTitleGenerated;
import static net.coosanta.llm.utility.WebUtils.ping;

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

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> testOptions() {
        return ResponseEntity.ok().build();
    }

    // Bash: curl -H "Content-Type: application/json" -d 'You are a helpful and knowledgeable assistant.' http://localhost:8080/api/v1/initiate
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> startConversation(@RequestBody String systemPrompt) throws IOException {
        Conversation conversation = new Conversation(systemPrompt, llamaConfig);
        Map<String, String> response = new HashMap<>();
        response.put("uuid", conversation.getUuid().toString());
        return ResponseEntity.ok(response);
    }

    // Bash: curl -H "Content-Type: application/json" -d '{"prompt":"your-prompt-here"}' http://localhost:8080/api/v1/chat/your-uuid-here
    // TODO: FIX EMPTY RESPONSES
    @PostMapping(value = "/chat/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@PathVariable String id, @RequestBody String prompt) {
        try {
            return llamaApp.runConversation(UUID.fromString(id), prompt);
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to load conversation or generate response", e));
        }
    }

    // Bash (String): curl -X POST -H "Content-Type: application/json" -d '"your-string-input-here"' "http://localhost:8080/api/v1/complete?type=string"
    // Bash (Conversation): curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"your-system-prompt","messages":[{"role":"User","content":"your-message"}, {...}, {...}]}' "http://localhost:8080/api/v1/complete?type=conversation"
    // Bash (Also string): curl -X POST -H "Content-Type: application/json" -d '"your-string-input-here"' "http://localhost:8080/api/v1/complete"
    @PostMapping(value = "/complete", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completeChat(@RequestParam(required = false) String type, @RequestBody Object input) {
        SseEmitter emitter = new SseEmitter(0L);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            emitter.send(SseEmitter.event()
                    .name("generating"));

            Flux<String> response;

            if (type == null || Objects.equals(type.toLowerCase(), "string")) {
                response = llamaApp.completeString((String) input);
            } else if (Objects.equals(type.toLowerCase(), "conversation")) {
                System.out.println("Input conversation HashMap:\n" + input + "\n\n"); // Debug
                Conversation conversation = convertToConversation(input);

                assert conversation != null;
                System.out.println("Converted Conversation: \n" + conversation.toMap() + "\n\n");

                response = llamaApp.completeConversation(conversation);
            } else {
                emitter.completeWithError(new IllegalArgumentException("Invalid type: " + type));
                return emitter;
            }

            // Send response stream
            response.subscribe(
                    data -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(data));
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError,
                    () -> {
                        scheduler.close();
                        emitter.complete();
                    }
            );

            ping(scheduler, emitter);

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PutMapping("/edit/{convId}/{msgIndex}")
    public ResponseEntity<?> editMessage(
            @PathVariable String convId,
            @PathVariable int msgIndex,
            @RequestBody Message editedMessage) {
        try {
            Conversation conversation = getConversation(convId);
            conversation.editMessage(msgIndex, editedMessage.getRole(), editedMessage.getContent(), editedMessage.getTokenLength());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e);
        }
        return ResponseEntity.ok(editedMessage);
    }

    // Bash (set title): curl -X PUT "http://localhost:8080/api/v1/title?uuid=your-uuid-here&title=your-title-here"
    // Bash (generate title): curl -X PUT "http://localhost:8080/api/v1/title?uuid=your-uuid-here"
    @PutMapping("/title")
    public ResponseEntity<String> setConversationTitle(
            @RequestParam(required = false) String title,
            @RequestParam String uuid) throws IOException {

        Path conversationPath = getConversationSavePathFromUuid(UUID.fromString(uuid));
        Conversation conversation = getConversation(uuid);

        if (title == null) {
            if (conversation.getMessages().isEmpty()) {
                return ResponseEntity.badRequest().body("Cannot generate title, conversation is empty!");
            }

            CompletableFuture<String> futureTitle = CompletableFuture.supplyAsync(() -> {
                String generatedTitle = llamaApp.generateTitle(conversation);
                conversation.setTitle(generatedTitle);
                return generatedTitle;
            });
            title = futureTitle.join();
        } else {
            conversation.setTitle(title);
        }
        saveToFile(conversation, conversationPath);
        return ResponseEntity.ok(title);
    }

    // Bash: curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"your-system-prompt","messages":[{"role":"User","content":"your-message"}, {"role":"Assistant","content":"response-message"}]}' "http://localhost:8080/api/v1/completion-title"
    @PostMapping("/completion-title")
    public SseEmitter setTitleCompletion(@RequestBody Object conversation) {
        SseEmitter emitter = new SseEmitter(0L);

        // This is unreasonably complex for something this trivial.
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            emitter.send(SseEmitter.event()
                    .name("generating"));

            CompletableFuture.runAsync(() -> getCompletionTitleGenerated(conversation, emitter, llamaApp));

            ping(scheduler, emitter);

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PutMapping("/ignore/{convId}/{msgIndex}")
    public ResponseEntity<?> ignoreMessage(
            @PathVariable String convId,
            @PathVariable int msgIndex,
            @RequestBody boolean ignore) throws IOException {
        llamaApp.setIgnoreMessage(getConversation(convId), msgIndex, ignore);
        return ResponseEntity.ok(ignore);
    }

    @GetMapping("/regenerate/{id}")
    public Flux<String> regenerateLatest(@PathVariable String id) throws IOException {
        try {
            Conversation conversation = getConversation(id);
            int index = conversation.getMessages().size() - 1;
            llamaApp.setIgnoreMessage(conversation, index, true);
        } catch (Exception e) {
            return Flux.error(e);
        }

        return llamaApp.runConversation(UUID.fromString(id), null);
    }

    // Bash (all): curl -X GET "http://localhost:8080/api/v1/conversation/all"
    // Bash (specific):curl -X GET "http://localhost:8080/api/v1/conversation/{uuid}"
    @GetMapping("/conversation/{id}")
    public ResponseEntity<?> getConversations(@PathVariable String id) {
        try {
            if (Objects.equals(id, "all")) {
                return ResponseEntity.ok(getAllConversationsWithoutMessages());
            } else {
                return ResponseEntity.ok(loadFromFile(getConversationSavePathFromUuid(UUID.fromString(id))));
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to load conversation with id: " + id);
        }
    }

}