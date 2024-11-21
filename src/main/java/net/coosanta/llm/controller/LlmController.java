package net.coosanta.llm.controller;

import net.coosanta.llm.LlamaApp;
import net.coosanta.llm.LlamaConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Ignore IDE warnings about these classes, they are used and are quite important!
@RestController
@RequestMapping("/api")
public class LlmController {
    public final LlamaConfig llamaConfig;

    public LlmController(LlamaConfig llamaConfig) {
        this.llamaConfig = llamaConfig;
    }

    // PowerShell: Invoke-RestMethod -Uri "http://localhost:8080/api/generate" -Method Post -Headers @{ "Content-Type" = "application/json" } -Body '"You are a helpful and knowledgeable assistant."'
    // Bash: curl -H "Content-Type: application/json" -d 'You are a helpful and knowledgeable assistant.' http://localhost:8080/api/generate
    @PostMapping("/generate")
    public ResponseEntity<LlamaApp> generateResponse(@RequestBody String prompt) {
        LlamaApp llamaApp = new LlamaApp(prompt, llamaConfig);
        return ResponseEntity.ok(llamaApp);
    }
}
