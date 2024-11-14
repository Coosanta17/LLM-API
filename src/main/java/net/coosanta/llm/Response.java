package net.coosanta.llm;

public class Response {
    private String prompt;
    private String response;
    private int id;

    public Response(){}

    public Response(String input, String inPromt) {
        this.response = input;
        this.prompt = inPromt;

        id = generateID();
    }

    private int generateID() {
        return (int)Math.floor(Math.random() * Integer.MAX_VALUE);
    }
}
