package com.grow.lyy.garden.controller;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.garden.entity.Garden;
import com.grow.lyy.garden.service.GardenService;
import com.grow.lyy.garden.vo.GardenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/garden")
@RequiredArgsConstructor
public class GardenController {

    private final GardenService gardenService;

    @GetMapping("/info")
    public Result<GardenVO> info() {

        return Result.success(
                gardenService.getCurrentGarden()
        );
    }

}