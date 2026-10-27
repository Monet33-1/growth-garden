package com.grow.lyy.diary.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("diary")
public class Diary {


    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 用户id
     */
    private Long userId;


    /**
     * 日记标题
     */
    private String title;


    /**
     * 日记内容
     */
    private String content;


    /**
     * 情绪（预留）
     */
    private String mood;


    /**
     * 日记日期
     */
    private LocalDate diaryDate;


    private LocalDateTime createTime;


    private LocalDateTime updateTime;

}