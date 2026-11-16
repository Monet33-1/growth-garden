
package com.grow.lyy.diary.controller;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.common.utils.SecurityUtils;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 创建随笔
     */
    @PostMapping
    public Result<?> create(
            @RequestBody Diary diary
    ) {

        Long userId =
                SecurityUtils.getUserId();

        return Result.success(diaryService.createDiary(
                userId,
                diary
        ));
    }

    /**
     * 查询自己的随笔
     */
    @GetMapping("/list")
    public Result<?> list() {

        Long userId =
                SecurityUtils.getUserId();

        return Result.success(
                diaryService.getMyDiary(userId)
        );
    }

    /**
     * 随机抽取一篇旧随笔，可带已看过的 ID 和情绪筛选。
     */
    @GetMapping("/random")
    public Result<?> random(
            @RequestParam(value = "exclude", required = false) String exclude,
            @RequestParam(value = "emotionId", required = false) Long emotionId
    ) {

        Long userId =
                SecurityUtils.getUserId();

        return Result.success(
                diaryService.getRandomMemory(
                        userId,
                        parseExcludeIds(exclude),
                        emotionId
                )
        );
    }

    private List<Long> parseExcludeIds(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();

        for (String part : raw.split(",")) {
            if (ids.size() >= 500) {
                break;
            }
            try {
                long id = Long.parseLong(part.trim());
                if (id > 0) {
                    ids.add(id);
                }
            }
            catch (NumberFormatException ignored) {
                // Ignore malformed ids instead of failing the whole request.
            }
        }

        return ids;
    }

    /**
     * 查询随笔详情
     */
    @GetMapping("/{id}")
    public Result<?> detail(
            @PathVariable Long id
    ) {

        Long userId =
                SecurityUtils.getUserId();

        return Result.success(
                diaryService.getDiaryDetail(
                        userId,
                        id
                )
        );
    }

    /**
     * 修改随笔
     */
    @PutMapping("/{id}")
    public Result<?> update(
            @PathVariable Long id,
            @RequestBody Diary diary
    ) {

        Long userId =
                SecurityUtils.getUserId();

        diaryService.updateDiary(
                userId,
                id,
                diary
        );

        return Result.success();
    }

    /**
     * 删除随笔
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @PathVariable Long id
    ) {

        Long userId =
                SecurityUtils.getUserId();

        diaryService.deleteDiary(
                userId,
                id
        );

        return Result.success();
    }
}

