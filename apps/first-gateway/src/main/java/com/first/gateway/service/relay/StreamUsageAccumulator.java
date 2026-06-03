package com.first.gateway.service.relay;

/**
 * Mutable holder for stream usage parsed from the last SSE chunk that includes usage.
 */
final class StreamUsageAccumulator {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    void update(int prompt, int completion, int total) {
        promptTokens = prompt;
        completionTokens = completion;
        totalTokens = total;
    }

    int promptTokens() {
        return promptTokens;
    }

    int completionTokens() {
        return completionTokens;
    }

    int totalTokens() {
        return totalTokens;
    }
}
