package net.coosanta.llm;

import org.junit.jupiter.api.Test;

import static net.coosanta.llm.ConversationUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversationUtilsTests {
    @Test
    void formatMessage_formatsMessageCorrectly() {
        String formattedMessage = formatMessage("user", "Hello");
        assertEquals("<|start_header_id|>user<|end_header_id|>\n\nHello<|eot_id|>", formattedMessage);
    }

    @Test
    void unformatMessage_unformatsMessageCorrectly() {
        String formattedMessage = "<|start_header_id|>user<|end_header_id|>\n\nHello<|eot_id|>";
        String content = unformatMessage(formattedMessage);
        assertEquals("Hello", content);
    }

    @Test
    void unformatMessage_handlesEmptyMessage() {
        String formattedMessage = "<|start_header_id|>user<|end_header_id|>\n\n<|eot_id|>";
        String content = unformatMessage(formattedMessage);
        assertEquals("", content);
    }


    // Testing empty conversation
    @Test
    void testGenerateContextWithoutMessages() {
        Conversation conversation = new Conversation();
        conversation.setSystemPrompt("This is a system prompt.");

        String context = generateContext(conversation);

        String expectedContext = "<|start_header_id|>System<|end_header_id|>\n\nThis is a system prompt.<|eot_id|><|start_header_id|>Assistant<|end_header_id|>\n\n";
        assertEquals(expectedContext, context);
    }

    // Testing small conversation
    @Test
    void testGenerateContextWithMessages() {
        Conversation conversation = new Conversation();
        conversation.setSystemPrompt("This is a system prompt.");
        conversation.addMessage("User", "Hello!", null);
        conversation.addMessage("Assistant", "Hi there!", null);
        conversation.addMessage("User", "mmmMMMMMmmmmMMMm m ", null);

        String context = generateContext(conversation);

        String expectedContext = """
                <|start_header_id|>System<|end_header_id|>
                
                This is a system prompt.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                Hello!<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                Hi there!<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                mmmMMMMMmmmmMMMm m <|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                """;
        assertEquals(expectedContext, context);
    }

    // Testing ignored messages
    @Test
    void testGenerateContextWithIgnoredMessages() {
        Conversation conversation = new Conversation();
        conversation.setSystemPrompt("This is a system prompt.");
        conversation.addMessage("User", "Hello!", null);
        conversation.addMessage("Assistant", "Hi there!", null);
        conversation.getMessages().get(1).setIgnored(true);

        String context = generateContext(conversation);

        String expectedContext = """
                <|start_header_id|>System<|end_header_id|>
                
                This is a system prompt.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                Hello!<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                """;
        assertEquals(expectedContext, context);
    }

    // Testing very large conversation with lots of text and messages
    @Test
    void testGenerateContextWithLargeConversation() {
        Conversation conversation = new Conversation();
        conversation.setSystemPrompt("This is a system prompt.");
        conversation.addMessage("User", "Hello!", null);
        conversation.addMessage("Assistant", "Hi there!", null);
        conversation.addMessage("User", "How are you?", null);
        conversation.addMessage("Assistant", "I'm doing well, thank you for asking.", null);
        conversation.addMessage("User", "That's good to hear.", null);
        conversation.addMessage("Assistant", "Yes, it is.", null);
        conversation.addMessage("User", "I have a question.", null);
        conversation.addMessage("Assistant", "What is your question?", null);
        conversation.addMessage("User", "What is the meaning of life?", null);
        conversation.addMessage("Assistant", """
                That's a profound and age-old question! Philosophers, scientists, and thinkers have grappled with this question for centuries, and there's no one definitive answer. However, here are some thoughts and perspectives to consider:
                
                Existentialism: According to existentialist philosophers like Jean-Paul Sartre, life has no inherent meaning. It's up to each individual to create their own purpose and give their life meaning through their choices and actions.
                Hedonic Utilitarianism: From a more practical perspective, some argue that the meaning of life is to seek happiness and fulfillment. This can be achieved by maximizing pleasure and minimizing pain, while also promoting the well-being of others.
                Spiritual or Religious Perspectives: Many spiritual and religious traditions believe that life has a deeper, transcendent purpose. For example, in Christianity, the meaning of life is often seen as serving God and fulfilling one's spiritual potential. Similarly, in Buddhism, the purpose of life is to attain enlightenment and end the cycle of suffering.
                Scientific and Evolutionary Perspectives: From a scientific standpoint, some argue that life's purpose is simply to survive, reproduce, and perpetuate the species. This view is supported by evolutionary theory and the concept of natural selection.
                Personal Growth and Self-Actualization: Others propose that the meaning of life is to reach one's full potential, pursue individual growth, and self-actualize. This view emphasizes the importance of personal development, learning, and self-expression.
                Remember, these perspectives are not mutually exclusive, and many people find meaning in life by integrating elements from multiple viewpoints.
                
                What resonates with you, or is there a specific perspective you'd like to explore further?""", null);
        conversation.addMessage("User", "That's a good answer.", null);
        conversation.addMessage("Assistant", "Thank you.", null);
        conversation.addMessage("User", "Goodbye!", null);

        String context = generateContext(conversation);

        String expectedContext = """
                <|start_header_id|>System<|end_header_id|>
                
                This is a system prompt.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                Hello!<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                Hi there!<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                How are you?<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                I'm doing well, thank you for asking.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                That's good to hear.<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                Yes, it is.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                I have a question.<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                What is your question?<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                What is the meaning of life?<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                That's a profound and age-old question! Philosophers, scientists, and thinkers have grappled with this question for centuries, and there's no one definitive answer. However, here are some thoughts and perspectives to consider:
                
                Existentialism: According to existentialist philosophers like Jean-Paul Sartre, life has no inherent meaning. It's up to each individual to create their own purpose and give their life meaning through their choices and actions.
                Hedonic Utilitarianism: From a more practical perspective, some argue that the meaning of life is to seek happiness and fulfillment. This can be achieved by maximizing pleasure and minimizing pain, while also promoting the well-being of others.
                Spiritual or Religious Perspectives: Many spiritual and religious traditions believe that life has a deeper, transcendent purpose. For example, in Christianity, the meaning of life is often seen as serving God and fulfilling one's spiritual potential. Similarly, in Buddhism, the purpose of life is to attain enlightenment and end the cycle of suffering.
                Scientific and Evolutionary Perspectives: From a scientific standpoint, some argue that life's purpose is simply to survive, reproduce, and perpetuate the species. This view is supported by evolutionary theory and the concept of natural selection.
                Personal Growth and Self-Actualization: Others propose that the meaning of life is to reach one's full potential, pursue individual growth, and self-actualize. This view emphasizes the importance of personal development, learning, and self-expression.
                Remember, these perspectives are not mutually exclusive, and many people find meaning in life by integrating elements from multiple viewpoints.
                
                What resonates with you, or is there a specific perspective you'd like to explore further?<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                That's a good answer.<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                Thank you.<|eot_id|><|start_header_id|>User<|end_header_id|>
                
                Goodbye!<|eot_id|><|start_header_id|>Assistant<|end_header_id|>
                
                """;
        assertEquals(expectedContext, context);
    }
}