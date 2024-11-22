package net.coosanta.llm.controller;

import net.coosanta.llm.ChatRequest;
import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaApp;
import net.coosanta.llm.LlamaConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Ignore IDE warnings about these classes, they are used and are quite important!
@RestController
@RequestMapping("/api")
public class LlmController {
    public final LlamaConfig llamaConfig;

    public LlmController(LlamaConfig llamaConfig) {
        this.llamaConfig = llamaConfig;
    }

    // PowerShell: Invoke-RestMethod -Uri "http://localhost:8080/api/initiate" -Method Post -Headers @{ "Content-Type" = "application/json" } -Body '"You are a helpful and knowledgeable assistant."'
    // Bash: curl -H "Content-Type: application/json" -d 'You are a helpful and knowledgeable assistant.' http://localhost:8080/api/initiate
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> startConversation(@RequestBody String systemPrompt) throws IOException {
        Conversation conversation = new Conversation(systemPrompt, llamaConfig);
        Map<String, String> response = new HashMap<>();
        response.put("uuid", conversation.getUuid().toString());
        return ResponseEntity.ok(response);
    }

    // Powershell: Invoke-RestMethod -Uri "http://localhost:8080/api/chat" -Method Post -Headers @{ "Content-Type" = "application/json" } -Body '{"id": "<uuid>", "prompt": "<prompt>"}'
    // Bash: curl -H "Content-Type: application/json" -d '{"id": "<uuid>", "prompt": "<prompt>"}' http://localhost:8080/api/chat
    @PostMapping("/chat")
    public ResponseEntity<SseEmitter> generateResponse(@RequestBody ChatRequest chatRequest) {
        SseEmitter emitter = new SseEmitter();
        UUID conversationId;

        try {
            conversationId = UUID.fromString(chatRequest.id());
        } catch (IllegalArgumentException e) {
            emitter.completeWithError(new RuntimeException("Invalid UUID format"));
            return ResponseEntity.badRequest().body(emitter);
        }

        try {
            LlamaApp llamaApp = new LlamaApp(conversationId, chatRequest.prompt(), llamaConfig, data -> {
                try {
                    emitter.send(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    emitter.completeWithError(e);
                }
            });
        } catch (IOException e) {
            try {
                emitter.send(SseEmitter.event().data("Conversation does not exist").name("error"));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            emitter.completeWithError(new RuntimeException("Conversation does not exist"));
            return ResponseEntity.status(404).body(emitter);
        }

        return ResponseEntity.ok(emitter);
    }

}
