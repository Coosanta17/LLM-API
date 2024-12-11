package net.coosanta.llm;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.coosanta.llm.ConversationUtils.*;

// Ignore IDE warnings about these classes, they are used and are quite important!
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

    // Bash: curl -H "Content-Type: application/json" -d 'You are a helpful and knowledgeable assistant.' http://localhost:8080/api/v1/initiate
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> startConversation(@RequestBody String systemPrompt) throws IOException {
        Conversation conversation = new Conversation(systemPrompt, llamaConfig);
        Map<String, String> response = new HashMap<>();
        response.put("uuid", conversation.getUuid().toString());
        return ResponseEntity.ok(response);
    }

    // Bash: curl -H "Content-Type: application/json" -d '{"prompt":"your-prompt-here"}' http://localhost:8080/api/v1/chat/your-uuid-here
    @PostMapping(value = "/chat/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@PathVariable String id, @RequestBody String prompt) {
        try {
            return llamaApp.runConversation(UUID.fromString(id), prompt);
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to load conversation or generate response", e));
        }
    }

    @PostMapping("/complete")
    public Flux<String> completeChat(@RequestBody Object input) {
        return llamaApp.complete(input);
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
