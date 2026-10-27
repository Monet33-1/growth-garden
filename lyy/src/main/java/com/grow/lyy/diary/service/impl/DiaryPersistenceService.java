package com.grow.lyy.diary.service.impl;

import com.grow.lyy.common.utils.MarkdownUtils;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.service.EmotionService;
import com.grow.lyy.garden.service.GardenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Persists a diary and its related garden state atomically.
 */
@Service
@RequiredArgsConstructor
public class DiaryPersistenceService {

    private final DiaryMapper diaryMapper;
    private final EmotionService emotionService;
    private final GardenService gardenService;

    @Transactional(rollbackFor = Exception.class)
    public DiaryVO persistDiary(
            Long userId,
            Diary diary,
            List<String> emotionNames
    ) {
        diary.setUserId(userId);
        if (diary.getCreateTime() == null) {
            diary.setCreateTime(LocalDateTime.now());
        }
        diaryMapper.insert(diary);

        if (emotionNames != null) {
            for (String emotionName : emotionNames) {
                Emotion emotion = emotionService.getByName(emotionName);
                if (emotion != null) {
                    emotionService.addDiaryEmotion(userId, diary.getId(), emotion.getId());
                }
            }
        }

        gardenService.addExperience(userId, 1);

        DiaryVO result = new DiaryVO();
        result.setId(diary.getId());
        result.setTitle(diary.getTitle());
        result.setContent(diary.getContent());
        result.setContentHtml(MarkdownUtils.toHtml(diary.getContent()));
        result.setMood(diary.getMood());
        result.setCreateTime(diary.getCreateTime());
        result.setEmotions(emotionService.getDiaryEmotions(userId, diary.getId()));
        return result;
    }
}
