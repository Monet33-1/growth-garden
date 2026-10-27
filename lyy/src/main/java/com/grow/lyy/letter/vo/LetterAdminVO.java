package com.grow.lyy.letter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LetterAdminVO {

    private Long id;
    private String title;
    private String content;
    private String unlockType;
    private Integer unlockValue;
    private String cover;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
