package net.coosanta.llm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class MainTest {

    @Test
    public void testMainWithValidPrompt() throws IOException {
        String[] args = {};
        Main.main(args);
        Generate generate = new Generate("Hello world!");
        String result = generate.responseMaker("Hello world!");
        assertEquals("Hello world!", result);
    }

    @Test
    public void testMainWithEmptyPrompt() throws IOException {
        String[] args = {};
        Main.main(args);
        Generate generate = new Generate("");
        String result = generate.responseMaker("");
        assertEquals("", result);
    }

    @Test
    public void testMainWithNullPrompt() {
        assertThrows(NullPointerException.class, () -> {
            String[] args = {};
            Main.main(args);
            Generate generate = new Generate(null);
            generate.responseMaker(null);
        });
    }
}