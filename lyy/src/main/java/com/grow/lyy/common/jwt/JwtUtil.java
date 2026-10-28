package com.grow.lyy.common.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;


@Slf4j
@Component
public class JwtUtil {


    @Value("${jwt.secret:}")
    private String secret;

    private volatile String generatedSecret;


    private final long EXPIRATION =
            1000 * 60 * 60 * 24 * 7;



    private Key getKey(){

        String value = secret;
        if (value == null || value.isBlank()) {
            if (generatedSecret == null) {
                byte[] bytes = new byte[48];
                new SecureRandom().nextBytes(bytes);
                generatedSecret = Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);
                log.warn("jwt.secret 未配置，使用临时密钥；应用重启后需要重新登录。");
            }
            value = generatedSecret;
        }

        return Keys.hmacShaKeyFor(
                value.getBytes(StandardCharsets.UTF_8)
        );

    }



    public String generateToken(Long userId){


        return Jwts.builder()

                .setSubject(
                        String.valueOf(userId)
                )

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION
                        )
                )

                .signWith(
                        getKey(),
                        SignatureAlgorithm.HS256
                )

                .compact();

    }

    public Long getUserId(String token){

        Claims claims =
                Jwts.parserBuilder()

                        .setSigningKey(
                                getKey()
                        )

                        .build()

                        .parseClaimsJws(token)

                        .getBody();
        return Long.valueOf(
                claims.getSubject()
        );

    }

}
