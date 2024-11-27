package net.coosanta.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

// Static methods to do actions to conversations.
public class ConversationUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static LlamaConfig settings = LlmController.llamaConfig;

    public static String toJson(Conversation conversation) {
        return gson.toJson(conversation);
    }

    public static Conversation fromJson(String json) {
        return gson.fromJson(json, Conversation.class);
    }

    public static void saveToFile(Conversation conversation, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
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
        // Use regex to extract only the content between the role headers and the end token
        return formattedMessage
                .replaceAll("<\\|start_header_id\\|>.*?<\\|end_header_id\\|>\n", "") // Remove role header
                .replaceAll("<\\|eot_id\\|>", "") // Remove end-of-text token
                .trim(); // Trim extra whitespace
    }

    public static Path getConversationSavePathFromUuid(UUID uuid) {
        return Path.of(settings.getConversationPath() + "/" + uuid + ".json");
    }

    public static List<Conversation> getAllConversations() throws IOException {
        List<Conversation> conversations = new ArrayList<>();
        Path conversationsDir = Paths.get(settings.getConversationPath());
        Logger logger = Logger.getLogger(ConversationUtils.class.getName());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(conversationsDir, "*.json")) {
            for (Path entry : stream) {
                try {
                    conversations.add(loadFromFile(entry));
                } catch (IOException e) {
                    logger.warning("Failed to load conversation from file: " + entry + " - " + e.getMessage());
                }
            }
        }

        return conversations;
    }

}

