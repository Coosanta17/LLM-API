package net.coosanta.llm;

public class Prompt {
    private String prompt;
    private Chat chat;

    public Prompt(){
        // Click to add text
    }

    public Prompt(String inputPromt){
        this.prompt = inputPromt;
    }

    public String getPrompt() {
        return prompt;
    }

    public Chat getChat() {
        return chat;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
