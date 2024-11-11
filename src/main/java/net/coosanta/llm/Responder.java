package net.coosanta.llm;

public class Responder {
    private final Prompt prompt;

    public Responder(Prompt inputPrompt) {
        if (inputPrompt == null) {
            throw new IllegalArgumentException("inputPrompt cannot be null");
        }
        this.prompt = inputPrompt;
    }

    public String respond() {
        return "You sent: " + prompt.getPrompt();
    }

    // Debugging
    @Override
    public String toString() {
        return "Responder{" +
                "prompt=" + prompt.getPrompt() +
                '}';
    }
}
