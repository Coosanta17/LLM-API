package net.coosanta.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LlmApiApplication {
	public static final String MODEL_PATH = "./Llama-3.1_8B.gguf";

	public static void main(String[] args) {
		SpringApplication.run(LlmApiApplication.class, args);
	}

}
