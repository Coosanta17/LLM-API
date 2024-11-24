package net.coosanta.llm;

public record ChatRequest(String id, String prompt, Integer buffer) {}
