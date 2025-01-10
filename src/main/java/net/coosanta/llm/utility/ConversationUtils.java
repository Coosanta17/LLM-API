package net.coosanta.llm.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.coosanta.llm.Conversation;
import net.coosanta.llm.LlamaConfig;
import net.coosanta.llm.LlmController;
import net.coosanta.llm.Message;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

// Static methods to do actions to conversations.
public class ConversationUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final LlamaConfig settings = LlmController.llamaConfig;

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
        return "<|start_header_id|>" + role + "<|end_header_id|>\n\n" + content + "<|eot_id|>";
    }

    public static String generateContext(Conversation conversation) {
        StringBuilder generatedContext = new StringBuilder();

        // Adds system prompt to context
        generatedContext.append(formatMessage("System", conversation.getSystemPrompt()));

        for (Message message : conversation.getMessages()) {
            if (message.isIgnored()) continue;
            generatedContext.append(formatMessage(message.getRole(), message.getContent()));
        }
        // Add Model prompt
        generatedContext.append("<|start_header_id|>Assistant<|end_header_id|>\n\n");

        return generatedContext.toString();
    }

    public static String unformatMessage(String formattedMessage) {
        // Use regex to extract only the content between the role headers and the end token
        // Example formatted message:
        // "<|start_header_id|>User<|end_header_id|>\n\nHello, how are you?<|eot_id|>"
        return formattedMessage
                .replaceAll("<\\|start_header_id\\|>.*?<\\|end_header_id\\|>\n\n", "") // Remove role header
                .replaceAll("<\\|eot_id\\|>", "") // Remove end-of-text token
                .trim(); // Trim extra whitespace
    }

    public static Path getConversationSavePathFromUuid(UUID uuid) {
        return Path.of(settings.getConversationPath() + "/" + uuid + ".json");
    }

    public static List<Conversation> getAllConversationsWithoutMessages() throws IOException {
        List<Conversation> conversations = new ArrayList<>();
        Path conversationsDir = Paths.get(settings.getConversationPath());
        Logger logger = Logger.getLogger(ConversationUtils.class.getName());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(conversationsDir, "*.json")) {
            for (Path entry : stream) {
                try {
                    Conversation conversation = loadFromFile(entry);
                    conversation.setMessages(new ArrayList<>()); // Clear messages
                    conversations.add(conversation);
                } catch (IOException e) {
                    logger.warning("Failed to load conversation from file: " + entry + " - " + e.getMessage());
                }
            }
        }

        return conversations;
    }

    public static Conversation convertToConversation(Object input) {
        if (input instanceof Map<?, ?> map) {
            if (map.keySet().stream().allMatch(key -> key instanceof String)) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> castedMap = (LinkedHashMap<String, Object>) map;
                return new Conversation(castedMap);
            }
        } else if (input instanceof Conversation) {
            return (Conversation) input;
        } else {
            throw new IllegalArgumentException("Invalid input type for conversation");
        }
        return null;
    }

    public static Conversation getConversation(String uuid) throws IOException {
        Path conversationPath = getConversationSavePathFromUuid(UUID.fromString(uuid));
        return loadFromFile(conversationPath);
    }

}

