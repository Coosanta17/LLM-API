package net.coosanta.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChatContext {
    private JsonArray context;

    public ChatContext() {
        context = new JsonArray();
    }

    public void addMessage(String role, String message) {
        JsonObject entry = new JsonObject();
        entry.addProperty("role", role);
        entry.addProperty("message", message);
        context.add(entry);
    }

    public String getContextAsString() {
        Gson gson = new Gson();
        return gson.toJson(context);
    }
}
