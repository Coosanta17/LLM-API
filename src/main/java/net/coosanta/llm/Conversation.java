package net.coosanta.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Conversation {
    private final UUID uuid;
    private String systemPrompt;
    private String title;
    private List<Message> messages;
    private int totalTokenLength;

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
        this.totalTokenLength = conversation.getTotalTokenLength();
    }

    public Conversation(LinkedHashMap<String, Object> map) {
        this.uuid = map.get("uuid") != null ? UUID.fromString((String) map.get("uuid")) : null;
        this.systemPrompt = Objects.requireNonNull((String) map.get("systemPrompt"), "systemPrompt cannot be null");
        this.title = (String) map.get("title");
        this.messages = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> messagesMap = (List<LinkedHashMap<String, Object>>) map.get("messages");

        for (LinkedHashMap<String, Object> messageMap : messagesMap) {
            this.messages.add(new Message(
                    Objects.requireNonNull((String) messageMap.get("role"), "role cannot be null"),
                    Objects.requireNonNull((String) messageMap.get("content"), "content cannot be null"),
                    (Integer) messageMap.get("tokenLength")
            ));
        }
        this.totalTokenLength = map.get("totalTokenLength") != null ? (Integer) map.get("totalTokenLength") : 0;
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

    public void addMessage(String role, String content, Integer tokenLength) {
        this.messages.add(new Message(role, content, tokenLength));
    }

    public void editMessage(int index, String role, String content, Integer tokenLength) {
        this.messages.set(index, new Message(role, content, tokenLength));
    }

    public Message getMessage(int index) {
        return this.messages.get(index);
    }

    public void setMessageIgnore(int index, boolean ignore) {
        this.messages.get(index).setIgnored(ignore);
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

    public int getTotalTokenLength() {
        return totalTokenLength;
    }

    public void setTotalTokenLength(int totalTokenLength) {
        this.totalTokenLength = totalTokenLength;
    }

    // Debug method
    public HashMap<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("systemPrompt", this.systemPrompt);
        map.put("title", this.title);
        map.put("messages", this.messages.stream().map(Message::toMap).collect(Collectors.toList()));
        return map;
    }
}

