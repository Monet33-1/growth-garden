package com.grow.lyy.diary.service.impl;

import com.grow.lyy.ai.service.EmotionClassifier;
import com.grow.lyy.ai.service.TitleGenerator;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.emotion.mapper.DiaryEmotionMapper;
import com.grow.lyy.emotion.service.EmotionService;
import com.grow.lyy.letter.mapper.UserLetterMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DiaryServiceImplTest {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final EmotionService emotionService = mock(EmotionService.class);
    private final DiaryPersistenceService diaryPersistenceService = mock(DiaryPersistenceService.class);
    private final TitleGenerator titleGenerator = mock(TitleGenerator.class);
    private final EmotionClassifier emotionClassifier = mock(EmotionClassifier.class);
    private final DiaryEmotionMapper diaryEmotionMapper = mock(DiaryEmotionMapper.class);
    private final UserLetterMapper userLetterMapper = mock(UserLetterMapper.class);

    private final DiaryServiceImpl service = new DiaryServiceImpl(
            diaryMapper,
            emotionService,
            diaryPersistenceService,
            titleGenerator,
            emotionClassifier,
            diaryEmotionMapper,
            userLetterMapper
    );

    @Test
    void deleteRemovesDiaryRelations() {
        Diary diary = new Diary();
        diary.setId(10L);
        diary.setUserId(1L);
        when(diaryMapper.selectOne(any())).thenReturn(diary);

        service.deleteDiary(1L, 10L);

        verify(diaryEmotionMapper).delete(any());
        verify(userLetterMapper).delete(any());
        verify(diaryMapper).deleteById(10L);
    }

    @Test
    void deleteRejectsAnotherUsersDiaryWithoutTouchingRelations() {
        when(diaryMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.deleteDiary(1L, 99L));

        verify(diaryMapper, never()).deleteById(anyLong());
        verifyNoInteractions(diaryEmotionMapper, userLetterMapper);
    }
}
