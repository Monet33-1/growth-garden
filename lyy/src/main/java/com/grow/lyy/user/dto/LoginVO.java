package com.grow.lyy.user.dto;

import lombok.Data;

@Data
public class LoginVO {


    private String token;


    private Long userId;


    private String nickname;

}