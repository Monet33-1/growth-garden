package com.grow.lyy.common.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/** Issues short-lived, unguessable administrator sessions held only in server memory. */
@Component
public class AdminSessionService {

    private static final long SESSION_SECONDS = 60 * 60;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Instant> sessions = new ConcurrentHashMap<>();

    public String createToken() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(digest(token), now.plusSeconds(SESSION_SECONDS));
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        String tokenDigest = digest(token);
        Instant expiry = sessions.get(tokenDigest);
        if (expiry == null) return false;
        if (!expiry.isAfter(Instant.now())) {
            sessions.remove(tokenDigest, expiry);
            return false;
        }
        return true;
    }

    private String digest(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("管理员会话暂不可用", e);
        }
    }
}
