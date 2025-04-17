package net.coosanta.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
public class LlamaConfig {
    private String conversationPath;
    private int buffer;
    private int loadedConversationsLimit;
    private boolean ssePing;
    private int pingInterval;
    private boolean saveCompletionConversations;

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

    public int getLoadedConversationsLimit() {
        return this.loadedConversationsLimit;
    }

    public void setLoadedConversationsLimit(int loadedConversationsLimit) {
        this.loadedConversationsLimit = loadedConversationsLimit;
    }

    public boolean isSsePing() {
        return this.ssePing;
    }

    public void setSsePing(boolean ssePing) {
        this.ssePing = ssePing;
    }

    public int getPingInterval() {
        return this.pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public boolean isSaveCompletionConversations() {
        return this.saveCompletionConversations;
    }

    public void setSaveCompletionConversations(boolean saveCompletionConversations) {
        this.saveCompletionConversations = saveCompletionConversations;
    }


    public ModelConfig getModelSettings() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }

    public static class ModelConfig {
        private String path;
        private int context;
        private int batchSize;
        private int responseLimit;
        private int threads;
        private int parallelSequences;
        private int gpuLayers;
        private int inactivityTimeout;
        private boolean loadOnStart;
        private Map<String, Float> loraAdapters = new HashMap<>();

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

        public int getBatchSize() {
            return this.batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getResponseLimit() {
            return this.responseLimit;
        }

        public void setResponseLimit(int responseLimit) {
            this.responseLimit = responseLimit;
        }

        public int getThreads() {
            return this.threads;
        }

        public void setThreads(int threads) {
            this.threads = threads;
        }

        public int getParallelSequences() {
            return this.parallelSequences;
        }

        public void setParallelSequences(int parallelSequences) {
            this.parallelSequences = parallelSequences;
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

        public Map<String, Float> getLoraAdapters() {
            return this.loraAdapters;
        }

        public void setLoraAdapters(Map<String, Float> loraAdapters) {
            this.loraAdapters = loraAdapters;
        }
    }
}