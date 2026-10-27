
package com.grow.lyy.emotion.service;

import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.emotion.entity.Emotion;

import java.util.List;

public interface EmotionService {

    void createEmotion(Emotion emotion);

    List<Emotion> getAllEmotions();

    Emotion getById(Long id);

    Emotion getByName(String name);

    void deleteEmotion(Long id);

    void addDiaryEmotion(Long userId, Long diaryId, Long emotionId);

    void removeDiaryEmotion(Long userId, Long diaryId, Long emotionId);

    List<Emotion> getDiaryEmotions(Long userId, Long diaryId);

    /**
     * 查询某个情绪瓶子里属于当前用户的随笔
     */
    List<DiaryVO> getEmotionDiaries(Long userId, Long emotionId);
}

