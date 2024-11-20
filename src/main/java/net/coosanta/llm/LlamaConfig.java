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
        private int gpuLayers;

        public String getPath() {
            return this.path;
        }

        public int getContext() {
            return this.context;
        }

        public int getThreads() {
            return this.threads;
        }

        public int getGpuLayers() {
            return this.gpuLayers;
        }
    }

    public ModelConfig getModelSettings() {
        return model;
    }
}