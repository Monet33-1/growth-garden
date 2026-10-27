package com.grow.lyy.garden.vo;

import lombok.Data;

@Data
public class GardenVO {

    /**
     * 花园名称
     */
    private String gardenName;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 当前经验
     */
    private Integer experience;

    /**
     * 成长阶段
     */
    private String stage;

    /**
     * 花园头像
     */
    private String avatar;

    /**
     * 背景图
     */
    private String background;

}