package net.coosanta.llm.controller;

import net.coosanta.llm.ChatRequest;
import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaApp;
import net.coosanta.llm.LlamaConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Bash: curl -H "Content-Type: application/json" -d '{"id": "<uuid>", "prompt": "<prompt>", "buffer": <see readme>}' http://localhost:8080/api/chat
    // Powershell: Invoke-RestMethod -Uri "http://localhost:8080/api/chat" -Method Post -Headers @{ "Content-Type" = "application/json" } -Body '{"id": "<uuid>", "prompt": "<prompt>", "buffer": <see readme>}'
    @PostMapping("/chat")
    public ResponseEntity<SseEmitter> generateResponse(@RequestBody ChatRequest chatRequest) {
        SseEmitter emitter = new SseEmitter((long) 60 * 60 * 1000); // 1-hour timeout

        emitter.onTimeout(() -> {
            System.err.println("SSE Timeout at " + DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now()));
            emitter.complete();
        });

        new Thread(streamResponse(chatRequest, emitter)).start();

        return ResponseEntity.ok(emitter);
    }

    // Helper methods:

    private @NotNull Runnable streamResponse(ChatRequest chatRequest, SseEmitter emitter) {
        return () -> {
            try {
                UUID conversationId = UUID.fromString(chatRequest.id());
                int bufferConfig = chatRequest.buffer() != null
                        ? chatRequest.buffer()
                        : llamaConfig.getBuffer();

                handleBuffering(bufferConfig, conversationId, chatRequest.prompt(), emitter);

                emitter.send(SseEmitter.event().data("[DONE]"));
            } catch (IllegalArgumentException e) {
                handleError(emitter, "Invalid UUID format", e);
            } catch (IOException e) {
                handleError(emitter, "Conversation does not exist", e);
            } finally {
                emitter.complete();
            }
        };
    }

    private void handleBuffering(int bufferConfig, UUID conversationId, String prompt, SseEmitter emitter) throws IOException {
        if (bufferConfig == 0) {
            // No buffer: stream each token
            new LlamaApp(conversationId, prompt, llamaConfig, data -> sendData(emitter, data));

        } else if (bufferConfig > 0) {
            // Buffered: send data in configured chunks
            StringBuilder buffer = new StringBuilder();
            new LlamaApp(conversationId, prompt, llamaConfig, data -> {
                buffer.append(data).append(" ");
                if (buffer.length() >= bufferConfig) {
                    sendData(emitter, buffer.toString().trim());
                    buffer.setLength(0);
                }
            });
            if (!buffer.isEmpty()) {
                sendData(emitter, buffer.toString().trim());
            }

        } else {
            // Fully buffered: send response after completion
            StringBuilder fullResponse = new StringBuilder();
            new LlamaApp(conversationId, prompt, llamaConfig, fullResponse::append);
            sendData(emitter, fullResponse.toString().trim());
        }
    }

    private void sendData(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            e.printStackTrace();
            emitter.completeWithError(e);
        }
    }

    private void handleError(SseEmitter emitter, String message, Exception e) {
        try {
            emitter.send(SseEmitter.event().data(message).name("error"));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        emitter.completeWithError(e);
    }

}
