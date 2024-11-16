package net.coosanta.llm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversationUtilsTests {
    @Test
    void formatMessage_formatsMessageCorrectly() {
        String formattedMessage = ConversationUtils.formatMessage("user", "Hello");
        assertEquals("<|start_header_id|>user<|end_header_id|>\nHello<|eot_id|>\n\n", formattedMessage);
    }

    @Test
    void unformatMessage_unformatsMessageCorrectly() {
        String formattedMessage = "<|start_header_id|>user<|end_header_id|>\nHello<|eot_id|>\n\n";
        String content = ConversationUtils.unformatMessage(formattedMessage);
        assertEquals("Hello", content);
    }

    @Test
    void unformatMessage_handlesEmptyMessage() {
        String formattedMessage = "<|start_header_id|>user<|end_header_id|>\n<|eot_id|>\n\n";
        String content = ConversationUtils.unformatMessage(formattedMessage);
        assertEquals("", content);
    }
}