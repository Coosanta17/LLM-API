package net.coosanta.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Conversation {
    private final UUID uuid;
    private String systemPrompt;
    private String title;
    private List<Message> messages;

    public Conversation(String systemPrompt, LlamaConfig settings) throws IOException {
        this.uuid = UUID.randomUUID();
        this.systemPrompt = systemPrompt;
        this.messages = new ArrayList<>();

        ConversationUtils.saveToFile(this, Path.of(settings.getConversationPath() + "/" + uuid + ".json"));
    }

    public Conversation(Conversation conversation) {
        this.uuid = conversation.getUuid();
        this.systemPrompt = conversation.getSystemPrompt();
        this.title = conversation.getTitle();
        this.messages = conversation.getMessages();
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

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(String role, String content) {
        this.messages.add(new Message(role, content));
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

