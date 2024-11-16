package net.coosanta.llm;

public class Prompt {
    private String content;
    private Chat chat;

    public Prompt(){
        // Click to add text
    }

    public Prompt(String inputPromt){
        this.content = inputPromt;
    }

    public String getContent() {
        return content;
    }

    public Chat getChat() {
        return chat;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
