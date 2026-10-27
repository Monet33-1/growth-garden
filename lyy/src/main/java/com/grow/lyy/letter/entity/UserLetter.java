package com.grow.lyy.letter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Per-user record of a preset letter saved into the garden. */
@Data
@TableName("user_letter")
public class UserLetter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long letterId;

    private Long diaryId;

    private LocalDateTime createTime;
}
