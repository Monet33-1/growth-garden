package com.grow.lyy.user.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("garden_user")
public class User {


    @TableId(
            type = IdType.AUTO
    )
    private Long id;


    private String username;


    private String password;


    private String nickname;


    private String avatar;


    private LocalDate birthday;


    private LocalDateTime createTime;


    private LocalDateTime updateTime;

}