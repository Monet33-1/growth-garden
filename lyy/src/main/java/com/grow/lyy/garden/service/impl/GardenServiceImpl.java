
package com.grow.lyy.garden.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grow.lyy.common.security.SecurityUtils;
import com.grow.lyy.garden.entity.Garden;
import com.grow.lyy.garden.mapper.GardenMapper;
import com.grow.lyy.garden.service.GardenService;
import com.grow.lyy.garden.vo.GardenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GardenServiceImpl implements GardenService {

    private final GardenMapper gardenMapper;


    @Override
    public void createGarden(Long userId) {

        Garden garden = new Garden();

        garden.setUserId(userId);
        garden.setGardenName("我的成长花园");
        garden.setLevel(1);
        garden.setExperience(0);
        garden.setStage("SEED");

        gardenMapper.insert(garden);
    }


    @Override
    public GardenVO getCurrentGarden() {

        Long userId =
                SecurityUtils.getCurrentUserId();

        Garden garden =
                getByUserId(userId);

        if (garden == null) {
            throw new RuntimeException("花园不存在");
        }

        GardenVO vo = new GardenVO();

        vo.setGardenName(garden.getGardenName());
        vo.setLevel(garden.getLevel());
        vo.setExperience(garden.getExperience());
        vo.setStage(garden.getStage());
        vo.setAvatar(garden.getAvatar());
        vo.setBackground(garden.getBackground());

        return vo;
    }


    @Override
    public Garden getByUserId(Long userId) {

        return gardenMapper.selectOne(
                new LambdaQueryWrapper<Garden>()
                        .eq(
                                Garden::getUserId,
                                userId
                        )
        );
    }


    @Override
    public void addExperience(
            Long userId,
            Integer experience
    ) {

        if (experience == null || experience <= 0) {
            return;
        }

        Garden garden =
                getByUserId(userId);

        if (garden == null) {
            throw new RuntimeException("花园不存在");
        }

        // 增加累计成长经验
        int newExperience =
                garden.getExperience() + experience;

        garden.setExperience(newExperience);

        // 根据累计经验计算等级
        int level =
                calculateLevel(newExperience);

        garden.setLevel(level);

        // 根据等级更新花园阶段
        String stage =
                calculateStage(level);

        garden.setStage(stage);

        // 保存
        gardenMapper.updateById(garden);
    }


    /**
     * 根据累计经验计算等级
     */
    private int calculateLevel(int experience) {

        return experience / 10 + 1;
    }


    /**
     * 根据等级计算花园阶段
     */
    private String calculateStage(int level) {

        return switch (level) {

            case 1 -> "SEED";

            case 2 -> "SPROUT";

            case 3 -> "SAPLING";

            case 4 -> "TREE";

            case 5 -> "BLOOM";

            default -> "BLOOM";
        };
    }
}

