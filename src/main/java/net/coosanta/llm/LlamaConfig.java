package net.coosanta.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class LlamaConfig {
    private String conversationPath;
    private ModelConfig model = new ModelConfig();

    public String getConversationPath() {
        // Removes trailing slashes if any
        return this.conversationPath != null ? this.conversationPath.replaceAll("/+$", "") : null;
    }

    public void setConversationPath(String conversationPath) {
        this.conversationPath = conversationPath;
    }

    public static class ModelConfig {
        private String path;
        private int context;
        private int threads;
        private int gpuLayers;

        public String getPath() {
            return this.path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getContext() {
            return this.context;
        }

        public void setContext(int context) {
            this.context = context;
        }

        public int getThreads() {
            return this.threads;
        }

        public void setThreads(int threads) {
            this.threads = threads;
        }

        public int getGpuLayers() {
            return this.gpuLayers;
        }

        public void setGpuLayers(int gpuLayers) {
            this.gpuLayers = gpuLayers;
        }
    }

    public ModelConfig getModelSettings() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }
}