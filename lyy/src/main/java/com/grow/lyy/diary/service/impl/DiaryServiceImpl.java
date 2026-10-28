
package com.grow.lyy.diary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grow.lyy.ai.AiUnavailableException;
import com.grow.lyy.ai.service.EmotionClassifier;
import com.grow.lyy.ai.service.TitleGenerator;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.utils.MarkdownUtils;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.diary.service.DiaryService;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.emotion.entity.DiaryEmotion;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.mapper.DiaryEmotionMapper;
import com.grow.lyy.emotion.service.EmotionService;
import com.grow.lyy.letter.entity.UserLetter;
import com.grow.lyy.letter.mapper.UserLetterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryMapper diaryMapper;

    private final EmotionService emotionService;

    private final DiaryPersistenceService diaryPersistenceService;

    private final TitleGenerator titleGenerator;

    private final EmotionClassifier emotionClassifier;

    private final DiaryEmotionMapper diaryEmotionMapper;

    private final UserLetterMapper userLetterMapper;

    @Override
    public DiaryVO createDiary(
            Long userId,
            Diary diary
    ) {


        if (diary == null) {
            throw new BusinessException("随笔不能为空");
        }


        if (diary.getContent() == null
                || diary.getContent().isBlank()) {

            throw new BusinessException(
                    "随笔内容不能为空"
            );
        }


        diary.setUserId(userId);



        // AI生成标题
        if(diary.getTitle() == null
                || diary.getTitle().isBlank()){


            String title =
                    titleGenerator.generateTitle(
                            diary.getContent()
                    );


            diary.setTitle(title);

        }



        // AI识别多个情绪；不可用时保存随笔，交给前端手动选择。
        List<String> emotionNames;
        boolean emotionFallbackRequired = false;
        try {
            emotionNames = emotionClassifier.classify(diary.getContent());
        }
        catch (AiUnavailableException e) {
            emotionNames = List.of();
            emotionFallbackRequired = true;
        }

        DiaryVO result = diaryPersistenceService.persistDiary(
                userId,
                diary,
                emotionNames
        );
        result.setEmotionFallbackRequired(emotionFallbackRequired);
        return result;

    }
    @Override
    public List<DiaryVO> getMyDiary(
            Long userId
    ) {

        List<Diary> diaries =
                diaryMapper.selectList(
                        new LambdaQueryWrapper<Diary>()
                                .eq(
                                        Diary::getUserId,
                                        userId
                                )
                                .orderByDesc(
                                        Diary::getCreateTime
                                )
                );

        return diaries.stream()
                .map(diary -> toVO(userId, diary))
                .toList();
    }

    @Override
    public long countDiary(
            Long userId
    ) {

        return diaryMapper.selectCount(
                Wrappers.<Diary>lambdaQuery()
                        .eq(
                                Diary::getUserId,
                                userId
                        )
        );
    }

    @Override
    public DiaryVO getDiaryDetail(
            Long userId,
            Long diaryId
    ) {

        Diary diary = getUserDiary(
                userId,
                diaryId
        );

        return toVO(userId, diary);
    }

    @Override
    public void updateDiary(
            Long userId,
            Long diaryId,
            Diary diary
    ) {

        if (diary == null) {
            throw new BusinessException("随笔不能为空");
        }

        if (diary.getContent() == null
                || diary.getContent().isBlank()) {
            throw new BusinessException("随笔内容不能为空");
        }

        Diary existingDiary =
                getUserDiary(
                        userId,
                        diaryId
                );

        existingDiary.setTitle(
                diary.getTitle()
        );

        existingDiary.setContent(
                diary.getContent()
        );

        existingDiary.setMood(
                diary.getMood()
        );

        existingDiary.setDiaryDate(
                diary.getDiaryDate()
        );

        diaryMapper.updateById(existingDiary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiary(
            Long userId,
            Long diaryId
    ) {

        Diary diary =
                getUserDiary(
                        userId,
                        diaryId
                );

        // Remove every relation that points at this diary before removing the diary itself.
        diaryEmotionMapper.delete(
                new LambdaQueryWrapper<DiaryEmotion>()
                        .eq(
                                DiaryEmotion::getDiaryId,
                                diary.getId()
                        )
        );

        // A diary created from a preset letter owns a user_letter row. Removing it lets
        // the letter become unread again and be collected anew later.
        userLetterMapper.delete(
                new LambdaQueryWrapper<UserLetter>()
                        .eq(
                                UserLetter::getUserId,
                                userId
                        )
                        .eq(
                                UserLetter::getDiaryId,
                                diary.getId()
                        )
        );

        diaryMapper.deleteById(
                diary.getId()
        );
    }

    /**
     * 查询属于当前用户的随笔
     */
    private Diary getUserDiary(
            Long userId,
            Long diaryId
    ) {

        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        if (diaryId == null) {
            throw new BusinessException("随笔不存在或无权访问");
        }

        Diary diary =
                diaryMapper.selectOne(
                        new LambdaQueryWrapper<Diary>()
                                .eq(
                                        Diary::getId,
                                        diaryId
                                )
                                .eq(
                                        Diary::getUserId,
                                        userId
                                )
                );

        if (diary == null) {
            throw new BusinessException(
                    "随笔不存在或无权访问"
            );
        }

        return diary;
    }

    /**
     * Diary 转 DiaryVO
     */
    private DiaryVO toVO(
            Long userId,
            Diary diary
    ) {

        DiaryVO vo =
                new DiaryVO();

        vo.setId(
                diary.getId()
        );

        vo.setTitle(
                diary.getTitle()
        );

        // Markdown 原文
        vo.setContent(
                diary.getContent()
        );

        // HTML 展示
        vo.setContentHtml(
                MarkdownUtils.toHtml(
                        diary.getContent()
                )
        );

        vo.setMood(
                diary.getMood()
        );

        vo.setCreateTime(
                diary.getCreateTime()
        );

        // 查询这篇随笔所属的情绪瓶子
        List<Emotion> emotions =
                emotionService.getDiaryEmotions(
                        userId,
                        diary.getId()
                );

        vo.setEmotions(emotions);

        return vo;
    }
}

