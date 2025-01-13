package net.coosanta.llm;

import net.coosanta.llm.utility.ConversationUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Conversation {
    private final UUID uuid;
    private String systemPrompt;
    private String name;
    private List<Message> messages;
    private int totalTokenLength;
    private Integer tokenLengthAtLastSystemPrompt;

    public Conversation() {
        this.uuid = UUID.randomUUID();
        this.messages = new ArrayList<>();
    }

    public Conversation(String systemPrompt, LlamaConfig settings) throws IOException {
        this.uuid = UUID.randomUUID();
        this.systemPrompt = systemPrompt;
        this.messages = new ArrayList<>();

        ConversationUtils.saveToFile(this, Path.of(settings.getConversationPath() + "/" + uuid + ".json"));
    }

    public Conversation(Conversation conversation) {
        this.uuid = conversation.getUuid() != null ? conversation.getUuid() : UUID.randomUUID();
        this.systemPrompt = conversation.getSystemPrompt();
        this.name = conversation.getName();
        this.messages = conversation.getMessages();
        this.totalTokenLength = conversation.getTotalTokenLength();
        this.tokenLengthAtLastSystemPrompt = conversation.getTokenLengthAtLastSystemPrompt();
    }

    public Conversation(LinkedHashMap<String, Object> map) {
        this.uuid = map.get("uuid") != null ? UUID.fromString((String) map.get("uuid")) : UUID.randomUUID();
        this.systemPrompt = Objects.requireNonNull((String) map.get("systemPrompt"), "systemPrompt cannot be null");
        this.name = (String) map.get("name");
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalTokenLength() {
        return totalTokenLength;
    }

    public void setTotalTokenLength(int totalTokenLength) {
        System.out.println("Setting total token length in conversation object");
        this.totalTokenLength = totalTokenLength;
    }

    public Integer getTokenLengthAtLastSystemPrompt() {
        return tokenLengthAtLastSystemPrompt;
    }

    public void setTokenLengthAtLastSystemPrompt(Integer tokenLengthAtLastSystemPrompt) {
        this.tokenLengthAtLastSystemPrompt = tokenLengthAtLastSystemPrompt;
    }
}

