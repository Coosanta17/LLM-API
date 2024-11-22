package net.coosanta.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Conversation {
    private UUID uuid;
    private String systemPrompt;
    private List<Message> messages;

    public Conversation(String systemPrompt, LlamaConfig settings) throws IOException {
        this.uuid = UUID.randomUUID();
        this.systemPrompt = systemPrompt;
        this.messages = new ArrayList<>();

        ConversationUtils.saveToFile(this, Path.of(settings.getConversationPath() + "/" + uuid + ".json"));
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

    public UUID getUuid() {
        return uuid;
    }
}

