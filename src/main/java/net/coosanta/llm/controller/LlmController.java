package net.coosanta.llm.controller;

import net.coosanta.llm.LlamaApp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Ignore IDE warnings about these classes, they are used and are quite important!
@RestController
@RequestMapping("/api")
public class LlmController {

//    @GetMapping("/getSomething")
//    public String getSomething() {
//        return "something";
//    }

//    // curl -H "Content-Type: application/json" -d '{"prompt": "Hello world!"}' http://localhost:port/api/testPrompt
//    // You sent: Hello world!
//    @PostMapping("/testPrompt")
//    public ResponseEntity<String> processPrompt(@RequestBody Prompt prompt) {
//        Responder response = new Responder(prompt);
//
//        String result = response.respond();
//        return ResponseEntity.ok(result);
//    }

    @PostMapping("/generate")
    public ResponseEntity<LlamaApp> generateResponse(@RequestBody String prompt) {
        LlamaApp llamaApp = new LlamaApp(prompt);
        return ResponseEntity.ok(llamaApp);
    }
}
