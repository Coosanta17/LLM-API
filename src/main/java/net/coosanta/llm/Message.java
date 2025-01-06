package net.coosanta.llm;

import java.util.HashMap;

public class Message {
    private String role;
    private String content;
    private Integer tokenLength;
    private boolean ignored;

    public Message(String role, String content, Integer tokenLength) {
        this.role = role;
        this.content = content;
        this.tokenLength = tokenLength;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenLength() {
        return tokenLength;
    }

    public void setTokenLength(Integer tokenLength) {
        this.tokenLength = tokenLength;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignore) {
        this.ignored = ignore;
    }

    // Debug method
    public HashMap<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("role", this.role);
        map.put("content", this.content);
        return map;
    }
}