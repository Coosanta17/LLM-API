package net.coosanta.llm.controller;

import net.coosanta.llm.Prompt;
import net.coosanta.llm.Responder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("./api")
public class LlmController {

//    @GetMapping("/getSomething")
//    public String getSomething() {
//        return "something";
//    }

    @PostMapping("/prompt")
    public ResponseEntity<String> processPrompt(@RequestBody Prompt prompt) {
        Responder response = new Responder(prompt);

        String result = response.respond();
        return ResponseEntity.ok(result);
    }
}
