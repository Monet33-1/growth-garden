
package com.grow.lyy.emotion.controller;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.common.security.AdminAuthorizationService;
import com.grow.lyy.common.utils.SecurityUtils;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.service.EmotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emotion")
@RequiredArgsConstructor
public class EmotionController {

    private final EmotionService emotionService;

    private final AdminAuthorizationService adminAuthorizationService;

    @PostMapping
    public Result<?> create(@RequestBody Emotion emotion) {
        adminAuthorizationService.requireAdmin();
        emotionService.createEmotion(emotion);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(
                emotionService.getAllEmotions()
        );
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(
                emotionService.getById(id)
        );
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        adminAuthorizationService.requireAdmin();
        emotionService.deleteEmotion(id);
        return Result.success();
    }

    @PostMapping("/diary/{diaryId}/{emotionId}")
    public Result<?> addDiaryEmotion(
            @PathVariable Long diaryId,
            @PathVariable Long emotionId) {

        Long userId = SecurityUtils.getUserId();

        emotionService.addDiaryEmotion(
                userId,
                diaryId,
                emotionId
        );

        return Result.success();
    }

    @DeleteMapping("/diary/{diaryId}/{emotionId}")
    public Result<?> removeDiaryEmotion(
            @PathVariable Long diaryId,
            @PathVariable Long emotionId) {

        Long userId = SecurityUtils.getUserId();

        emotionService.removeDiaryEmotion(
                userId,
                diaryId,
                emotionId
        );

        return Result.success();
    }

    @GetMapping("/diary/{diaryId}")
    public Result<?> getDiaryEmotions(
            @PathVariable Long diaryId) {

        Long userId = SecurityUtils.getUserId();

        return Result.success(
                emotionService.getDiaryEmotions(
                        userId,
                        diaryId
                )
        );
    }

    @GetMapping("/{emotionId}/diaries")
    public Result<?> getEmotionDiaries(
            @PathVariable Long emotionId) {

        Long userId = SecurityUtils.getUserId();

        return Result.success(
                emotionService.getEmotionDiaries(
                        userId,
                        emotionId
                )
        );
    }
}

