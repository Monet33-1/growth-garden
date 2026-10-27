package com.grow.lyy.emotion.service.impl;

import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.emotion.entity.DiaryEmotion;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.mapper.DiaryEmotionMapper;
import com.grow.lyy.emotion.mapper.EmotionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmotionServiceImplTest {

    private final EmotionMapper emotionMapper = mock(EmotionMapper.class);
    private final DiaryEmotionMapper diaryEmotionMapper = mock(DiaryEmotionMapper.class);
    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final EmotionServiceImpl service =
            new EmotionServiceImpl(emotionMapper, diaryEmotionMapper, diaryMapper);

    @Test
    void rejectsReadingEmotionsOfAnotherUsersDiary() {
        when(diaryMapper.selectCount(any())).thenReturn(0L);

        assertThrows(BusinessException.class,
                () -> service.getDiaryEmotions(1L, 99L));

        verify(diaryEmotionMapper, never()).selectList(any());
    }

    @Test
    void rejectsAddingEmotionToAnotherUsersDiary() {
        when(diaryMapper.selectCount(any())).thenReturn(0L);

        assertThrows(BusinessException.class,
                () -> service.addDiaryEmotion(1L, 99L, 5L));

        verify(diaryEmotionMapper, never()).insert(any(DiaryEmotion.class));
    }

    @Test
    void returnsOnlyTheCurrentUsersDiariesForAnEmotion() {
        Emotion emotion = new Emotion();
        emotion.setId(5L);
        emotion.setName("开心");
        when(emotionMapper.selectById(5L)).thenReturn(emotion);
        when(diaryEmotionMapper.selectList(any())).thenReturn(List.of(
                relation(10L, 5L),
                relation(20L, 5L)
        ));
        when(diaryMapper.selectList(any())).thenReturn(List.of(
                diary(10L, 1L, "我的随笔", "只属于我的内容")
        ));

        List<DiaryVO> result = service.getEmotionDiaries(1L, 5L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void returnsEmotionsForAnOwnedDiary() {
        when(diaryMapper.selectCount(any())).thenReturn(1L);
        when(diaryEmotionMapper.selectList(any())).thenReturn(List.of(
                relation(10L, 5L)
        ));
        when(emotionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                emotion(5L, "开心")
        ));

        List<Emotion> result = service.getDiaryEmotions(1L, 10L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
    }

    private Diary diary(Long id, Long userId, String title, String content) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setUserId(userId);
        diary.setTitle(title);
        diary.setContent(content);
        return diary;
    }

    private DiaryEmotion relation(Long diaryId, Long emotionId) {
        DiaryEmotion relation = new DiaryEmotion();
        relation.setDiaryId(diaryId);
        relation.setEmotionId(emotionId);
        return relation;
    }

    private Emotion emotion(Long id, String name) {
        Emotion emotion = new Emotion();
        emotion.setId(id);
        emotion.setName(name);
        return emotion;
    }
}
