package com.first.gateway.service.relay;

import com.first.gateway.service.auth.AuthService;

import java.util.Map;

public interface RelayService {

    Map<String, Object> chatCompletions(AuthService.AuthContext auth, Map<String, Object> request, long tpmReserved);

    void chatCompletionsStream(AuthService.AuthContext auth, Map<String, Object> request,
                               long tpmReserved, StreamConsumer consumer);

    @FunctionalInterface
    interface StreamConsumer {
        void accept(String chunk);
    }

    Map<String, Object> embeddings(AuthService.AuthContext auth, Map<String, Object> request, long tpmReserved);
}
