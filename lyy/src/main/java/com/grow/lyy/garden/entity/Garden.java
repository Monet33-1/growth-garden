package com.grow.lyy.garden.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("garden")
public class Garden {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String gardenName;

    private Integer level;

    private Integer experience;

    private String stage;

    private String avatar;

    private String background;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}