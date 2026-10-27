package com.grow.lyy.garden.service;

import com.grow.lyy.garden.entity.Garden;
import com.grow.lyy.garden.vo.GardenVO;

public interface GardenService {

    void createGarden(Long userId);

    GardenVO getCurrentGarden();

    Garden getByUserId(Long userId);

    /**
     * 增加花园成长经验
     */
    void addExperience(Long userId, Integer experience);
}