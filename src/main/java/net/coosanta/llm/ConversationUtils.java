package net.coosanta.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConversationUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(Conversation conversation) {
        return gson.toJson(conversation);
    }

    public static Conversation fromJson(String json) {
        return gson.fromJson(json, Conversation.class);
    }

    public static void saveToFile(Conversation conversation, Path filePath) throws IOException {
        String json = toJson(conversation);
        Files.writeString(filePath, json);
    }

    public static Conversation loadFromFile(Path filePath) throws IOException {
        String json = Files.readString(filePath);
        return fromJson(json);
    }

    // Llama wants messages to look like this
    public static String formatMessage(String role, String content) {
        return "<|start_header_id|>" + role + "<|end_header_id|>\n" + content + "<|eot_id|>\n\n";
    }

    public static String unformatMessage(String formattedMessage) {
        String[] parts = formattedMessage.split("<\\|end_header_id\\|>\\n|<\\|eot_id\\|>\\n\\n");
        if (parts.length >= 2) {
            return parts[1].trim();
        }
        return "";
    }
}

