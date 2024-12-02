package net.coosanta.llm;

public class Message {
    private String role;
    private String content;
    private Integer tokenLength;

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
}