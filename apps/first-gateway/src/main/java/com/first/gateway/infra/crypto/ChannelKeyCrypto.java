package com.first.gateway.infra.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChannelKeyCrypto {

    private final String secret;

    public ChannelKeyCrypto(@Value("${gateway.crypto.channel-key-secret}") String secret) {
        this.secret = secret;
    }

    public String encrypt(String plainText) {
        return plainText;
    }

    public String decrypt(String cipherText) {
        return cipherText;
    }
}
