package com.grow.lyy.letter.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("letter")
public class Letter {


    @TableId(type = IdType.AUTO)
    private Long id;


    private String title;


    private String content;


    /**
     * 解锁类型
     * DIARY_COUNT
     */
    private String unlockType;


    /**
     * 解锁条件值
     */
    private Integer unlockValue;


    private String cover;


    private Integer sort;


    private LocalDateTime createTime;


    private LocalDateTime updateTime;

}