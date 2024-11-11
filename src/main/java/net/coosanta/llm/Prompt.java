package net.coosanta.llm;

public class Prompt {
    private String prompt;

    public Prompt(String inputPromt){
        this.prompt = inputPromt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
