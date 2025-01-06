package net.coosanta.llm;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.coosanta.llm.ConversationUtils.*;

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
    public Flux<String> completeChat(@RequestParam(required = false) String type, @RequestBody Object input) {
        if (type == null || Objects.equals(type.toLowerCase(), "string")) {
            return llamaApp.completeString((String) input)
                    .mergeWith(pingStream()); // Add pings to keep the connection alive
        } else if (Objects.equals(type.toLowerCase(), "conversation")) {
            System.out.println("Input conversation HashMap:\n" + input + "\n\n"); // Debug
            Conversation conversation = convertToConversation(input);

            assert conversation != null;
            System.out.println("Converted Conversation: \n"+conversation.toMap()+"\n\n");

            return llamaApp.completeConversation(conversation)
                    .mergeWith(pingStream());
        } else {
            return Flux.error(new IllegalArgumentException("Invalid type: " + type));
        }
    }

    // Ping Stream to Keep Connection Alive
    private Flux<String> pingStream() {
        if (!settings.getSsePing()) {
            return Flux.empty();
        }
        return Flux.interval(Duration.ofSeconds(settings.getPingInterval()))
                .map(i -> "event: ping\n\n");
    }

    private Conversation convertToConversation(Object input) {
        if (input instanceof Map<?, ?> map) {
            if (map.keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> castedMap = (LinkedHashMap<String, Object>) map;
                return new Conversation(castedMap);
            }
        } else if (input instanceof Conversation) {
            return (Conversation) input;
        } else {
            throw new IllegalArgumentException("Invalid input type for conversation");
        }
        return null;
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

    private static Conversation getConversation(String uuid) throws IOException {
        Path conversationPath = getConversationSavePathFromUuid(UUID.fromString(uuid));
        return loadFromFile(conversationPath);
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

    @PostMapping("/completion-title")
    public ResponseEntity<String> setTitleCompletion(@RequestBody Conversation completion) {

        if (completion.getMessages().isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot generate title, conversation is empty!");
        }

        CompletableFuture<String> futureTitle = CompletableFuture.supplyAsync(() -> {
            String generatedTitle = llamaApp.generateTitle(completion);
            completion.setTitle(generatedTitle);
            return generatedTitle;
        });
        String title = futureTitle.join();
        return ResponseEntity.ok(title);
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
