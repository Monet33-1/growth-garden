package com.grow.lyy.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    private final AdminSessionService adminSessionService;

    private final String adminUsername;

    private final byte[] passwordSalt;

    private final byte[] passwordHash;

    private final int passwordIterations;

    private final boolean adminEnabled;

    private final boolean desktopUserAdmin;

    public AdminAuthorizationService(
            AdminSessionService adminSessionService,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password-salt:}") String passwordSalt,
            @Value("${app.admin.password-hash:}") String passwordHash,
            @Value("${app.admin.password-iterations:200000}") int passwordIterations,
            @Value("${app.desktop.user-admin:false}") boolean desktopUserAdmin
    ) {
        this.adminSessionService = adminSessionService;
        this.adminUsername = adminUsername == null ? "" : adminUsername;
        this.passwordIterations = passwordIterations;
        this.desktopUserAdmin = desktopUserAdmin;

        if (!this.adminUsername.isBlank()
                && passwordSalt != null && !passwordSalt.isBlank()
                && passwordHash != null && !passwordHash.isBlank()) {
            this.passwordSalt = Base64.getDecoder().decode(passwordSalt);
            this.passwordHash = Base64.getDecoder().decode(passwordHash);
            this.adminEnabled = true;
        }
        else {
            this.passwordSalt = null;
            this.passwordHash = null;
            this.adminEnabled = false;
        }
    }

    public String login(String username, String password) {
        if (!adminEnabled) {
            throw new AccessDeniedException("管理员功能未启用");
        }
        if (username == null || password == null || !matchesUsername(username) || !matchesPassword(password)) {
            throw new AccessDeniedException("管理员账号或密码不正确");
        }
        return adminSessionService.createToken();
    }

    public void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // The desktop build has exactly one local account, so the signed-in
        // owner can manage letters without a second administrator password.
        if (desktopUserAdmin
                && authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return;
        }

        if (!adminEnabled) {
            throw new AccessDeniedException("管理员功能未启用");
        }

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            throw new AccessDeniedException("管理员账号或密码不正确");
        }
    }

    private boolean matchesUsername(String username) {
        return MessageDigest.isEqual(
                adminUsername.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesPassword(String password) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, passwordIterations, passwordHash.length * 8);
        try {
            byte[] candidate = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return MessageDigest.isEqual(passwordHash, candidate);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("管理员密码校验暂不可用", e);
        } finally {
            spec.clearPassword();
        }
    }
}
