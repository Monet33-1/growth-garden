package com.grow.lyy.common.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {


    @Value("${jwt.secret}")
    private String secret;


    private final long EXPIRATION =
            1000 * 60 * 60 * 24 * 7;



    private Key getKey(){

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
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
