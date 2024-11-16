package net.coosanta.llm.controller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import net.coosanta.llm.Prompt;
import net.coosanta.llm.Responder;
import net.coosanta.llm.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
    public ResponseEntity<Response> generateResponse(@RequestBody Prompt prompt) throws IOException {
        Responder response = new Responder();
        return ResponseEntity.ok(response.respond());
    }
}
