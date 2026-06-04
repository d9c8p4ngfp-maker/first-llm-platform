package com.first.gateway.infra.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ChannelKeyCrypto {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LEN = 128;
    private static final int GCM_IV_LEN = 12;
    /** IV + at least one byte ciphertext + GCM tag */
    private static final int MIN_ENCRYPTED_LEN = GCM_IV_LEN + 17;

    private final byte[] key;

    public ChannelKeyCrypto(@Value("${gateway.crypto.channel-key-secret}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("gateway.crypto.channel-key-secret must be at least 16 chars");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            this.key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public boolean looksLikeEncrypted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= MIN_ENCRYPTED_LEN;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LEN + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LEN);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LEN, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (!looksLikeEncrypted(cipherText)) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LEN);
            byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LEN, combined.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_LEN, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decryption failed for encrypted channel key", e);
        }
    }
}
