package net.coosanta.llm;

public class Response {
    private String prompt;
    private String response;
    private int id;

    public Response(){}

    public Response(String inResponse, String inPromt) {
        this.response = inResponse;
        this.prompt = inPromt;

        id = generateID();
    }

    private int generateID() {
        return (int)Math.floor(Math.random() * Integer.MAX_VALUE);
    }
}
