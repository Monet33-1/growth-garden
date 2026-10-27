package com.grow.lyy.common.utils;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;



public class SecurityUtils {


    public static Long getUserId(){


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();



        if(authentication == null){
            throw new RuntimeException(
                    "用户未登录"
            );
        }


        return (Long) authentication.getPrincipal();

    }

}