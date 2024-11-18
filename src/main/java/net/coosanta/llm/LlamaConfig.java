package net.coosanta.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class LlamaConfig {
    private ModelConfig model = new ModelConfig();

    public static class ModelConfig {
        private String path;
        private int context;
        private int threads;
        private String seed;
        private int gpuLayers;

        private int findSeed(String inSeed) {
            int seedNum;
            if (seed.matches("^[0-9]*$")) {
                try {
                    seedNum = Integer.parseInt(inSeed);
                } catch (NumberFormatException e) {
                    System.err.println("Error reading seed from configuration - Unable to parse number");
                    throw new RuntimeException(e);
                }
            } else if (inSeed.equals("RANDOM")) {
                seedNum = (int) Math.floor(Math.random() * Integer.MAX_VALUE);
            } else {
                throw new RuntimeException("Error reading seed from configuration - Invalid option");
            }
            return seedNum;
        }

        public String getPath() {
            return this.path;
        }

        public int getContext() {
            return this.context;
        }

        public int getThreads() {
            return this.threads;
        }

        public int getSeed() {
            return findSeed(seed);
        }

        public int getGpuLayers() {
            return this.gpuLayers;
        }
    }

    public ModelConfig getModelSettings() {
        return model;
    }
}