package net.coosanta.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class LlamaConfig {
    private String conversationPath;
    private int buffer;
    private int loadedConversationLimit;
    private ModelConfig model = new ModelConfig();

    public String getConversationPath() {
        // Removes trailing slashes if any
        return this.conversationPath != null ? this.conversationPath.replaceAll("/+$", "") : null;
    }

    public void setConversationPath(String conversationPath) {
        this.conversationPath = conversationPath;
    }

    public int getBuffer() {
        return this.buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public int getLoadedConversationLimit() {
        return this.loadedConversationLimit;
    }

    public void setLoadedConversationLimit(int loadedConversationLimit) {
        this.loadedConversationLimit = loadedConversationLimit;
    }


    public static class ModelConfig {
        private String path;
        private int context;
        private int threads;
        private int gpuLayers;
        private int inactivityTimeout;
        private boolean loadOnStart;

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

        public int getInactivityTimeout() {
            return this.inactivityTimeout;
        }

        public void setInactivityTimeout(int inactivityTimeout) {
            this.inactivityTimeout = inactivityTimeout;
        }

        public boolean getLoadOnStart() {
            return this.loadOnStart;
        }

        public void setLoadOnStart(boolean loadOnStart) {
            this.loadOnStart = loadOnStart;
        }
    }

    public ModelConfig getModelSettings() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }
}