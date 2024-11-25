package net.coosanta.llm;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.coosanta.llm.ConversationUtils.getConversationSavePathFromUuid;
import static net.coosanta.llm.ConversationUtils.loadFromFile;

// Ignore IDE warnings about these classes, they are used and are quite important!
@RestController
@RequestMapping("/api/v1/")
public class LlmController {
    public static LlamaConfig llamaConfig;
    private LlamaApp llamaApp;

    public LlmController(LlamaConfig llamaConfig) throws IOException {
        LlmController.llamaConfig = llamaConfig;
        this.llamaApp = new LlamaApp(System.out::print); // Temporary fix. TODO: Improve model loading

    }

    // PowerShell: Invoke-RestMethod -Uri "http://localhost:8080/api/v1/initiate" -Method Post -Headers @{ "Content-Type" = "application/json" } -Body '"You are a helpful and knowledgeable assistant."'
    // Bash: curl -H "Content-Type: application/json" -d 'You are a helpful and knowledgeable assistant.' http://localhost:8080/api/v1/initiate
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> startConversation(@RequestBody String systemPrompt) throws IOException {
        Conversation conversation = new Conversation(systemPrompt, llamaConfig);
        Map<String, String> response = new HashMap<>();
        response.put("uuid", conversation.getUuid().toString());
        return ResponseEntity.ok(response);
    }

    // Powershell: Invoke-RestMethod -Uri "http://localhost:8080/api/v1/chat" -Method Post -ContentType "application/json" -Body '{"id": "your-uuid-here", "prompt": "your-prompt-here"}'
    // Bash: curl -H "Content-Type: application/json" -d '{"id":"your-uuid-here","prompt":"your-prompt-here"}' http://localhost:8080/api/v1/chat
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        try {
            return llamaApp.runConversation(UUID.fromString(request.id()), request.prompt());
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to load conversation or generate response", e));
        }
    }

    // NOT IDEAL!!! FIX ASAP!!!!
    @PutMapping("/title")
    public ResponseEntity<String> setConversationTitle(@RequestBody String title, @RequestBody String uuid) throws IOException {
        Conversation conversation = loadFromFile(getConversationSavePathFromUuid(UUID.fromString(uuid)));
        if (title == null) {
            new Thread(() -> {
                String generatedTitle = llamaApp.generateTitle(conversation);
                conversation.setTitle(generatedTitle);
            }).start();
        } else {
            conversation.setTitle(title);
        }
        return ResponseEntity.ok(title);
    }
}
