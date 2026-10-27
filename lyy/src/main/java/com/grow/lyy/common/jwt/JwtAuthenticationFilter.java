package com.grow.lyy.common.jwt;

import com.grow.lyy.common.security.AdminSessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import lombok.RequiredArgsConstructor;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.stereotype.Component;


import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.Collections;
import java.util.List;



@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {



    private final JwtUtil jwtUtil;
    private final AdminSessionService adminSessionService;



    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {



        String header =
                request.getHeader(
                        "Authorization"
                );


        if(header != null
                && header.startsWith("Bearer ")) {


            String token =
                    header.substring(7);



            try {
                boolean adminToken = adminSessionService.isValid(token);
                Object principal = adminToken ? 0L : jwtUtil.getUserId(token);
                var authorities = adminToken
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.<SimpleGrantedAuthority>emptyList();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities);



                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );



            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }


        }



        filterChain.doFilter(
                request,
                response
        );

    }

}
