package net.coosanta.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlamaConfig.class)
public class LlmApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(LlmApiApplication.class, args);
	}

}
