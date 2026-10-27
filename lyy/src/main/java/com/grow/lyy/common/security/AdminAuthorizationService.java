package com.grow.lyy.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/** Validates the dedicated credentials required for administrator-only operations. */
@Component
public class AdminAuthorizationService {

    private static final String ADMIN_USERNAME = "Threesheep";
    private static final byte[] PASSWORD_SALT = Base64.getDecoder().decode("VNFpsqaCwJW8NJ9DJ/UH8Q==");
    private static final byte[] PASSWORD_HASH = Base64.getDecoder().decode("c9YpzL351G2V1BsN6ji5yclDEW8yhyLlW8ZzVSqRVdk=");
    private static final int PASSWORD_ITERATIONS = 200_000;

    private final AdminSessionService adminSessionService;

    public AdminAuthorizationService(AdminSessionService adminSessionService) {
        this.adminSessionService = adminSessionService;
    }

    public String login(String username, String password) {
        if (username == null || password == null || !matchesUsername(username) || !matchesPassword(password)) {
            throw new AccessDeniedException("管理员账号或密码不正确");
        }
        return adminSessionService.createToken();
    }

    public void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            throw new AccessDeniedException("管理员账号或密码不正确");
        }
    }

    private boolean matchesUsername(String username) {
        return MessageDigest.isEqual(
                ADMIN_USERNAME.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesPassword(String password) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), PASSWORD_SALT, PASSWORD_ITERATIONS, PASSWORD_HASH.length * 8);
        try {
            byte[] candidate = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return MessageDigest.isEqual(PASSWORD_HASH, candidate);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("管理员密码校验暂不可用", e);
        } finally {
            spec.clearPassword();
        }
    }
}
