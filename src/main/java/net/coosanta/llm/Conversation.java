package net.coosanta.llm;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String systemPrompt;
    private List<Message> messages;

    public Conversation(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        this.messages = new ArrayList<>();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void addMessage(String role, String content) {
        this.messages.add(new Message(role, content));
    }
}

