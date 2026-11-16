package com.grow.lyy.diary.service.impl;

import com.grow.lyy.ai.AiUnavailableException;
import com.grow.lyy.ai.service.EmotionClassifier;
import com.grow.lyy.ai.service.TitleGenerator;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.diary.vo.RandomMemoryVO;
import com.grow.lyy.emotion.mapper.DiaryEmotionMapper;
import com.grow.lyy.emotion.service.EmotionService;
import com.grow.lyy.letter.mapper.UserLetterMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void marksEmotionFallbackWhenClassifierIsUnavailable() {
        Diary diary = new Diary();
        diary.setTitle("今天的风");
        diary.setContent("今天的花园很安静。");

        DiaryVO persisted = new DiaryVO();
        persisted.setId(11L);
        when(emotionClassifier.classify(any())).thenThrow(
                new AiUnavailableException("offline", new RuntimeException("offline"))
        );
        when(diaryPersistenceService.persistDiary(any(), any(), any()))
                .thenReturn(persisted);

        DiaryVO result = service.createDiary(1L, diary);

        assertTrue(result.isEmotionFallbackRequired());
    }

    @Test
    void randomMemoryAvoidsSeenDiariesAndResetsAfterPoolIsExhausted() {
        Diary first = new Diary();
        first.setId(1L);
        first.setUserId(1L);
        first.setTitle("第一封");
        first.setContent("第一篇");

        Diary second = new Diary();
        second.setId(2L);
        second.setUserId(1L);
        second.setTitle("第二封");
        second.setContent("第二篇");

        when(diaryMapper.selectList(any())).thenReturn(List.of(first, second));
        when(emotionService.getDiaryEmotions(anyLong(), anyLong())).thenReturn(List.of());

        RandomMemoryVO oneLeft = service.getRandomMemory(1L, List.of(1L), null);
        assertNotNull(oneLeft.getDiary());
        assertEquals(2L, oneLeft.getDiary().getId());
        assertFalse(oneLeft.isPoolReset());
        assertEquals(0, oneLeft.getRemaining());

        RandomMemoryVO reset = service.getRandomMemory(1L, List.of(1L, 2L), null);
        assertNotNull(reset.getDiary());
        assertTrue(reset.isPoolReset());
        assertEquals(1, reset.getRemaining());
    }

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
