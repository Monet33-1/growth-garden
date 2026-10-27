


package com.grow.lyy.emotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.utils.MarkdownUtils;
import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.mapper.DiaryMapper;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.emotion.entity.DiaryEmotion;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.mapper.DiaryEmotionMapper;
import com.grow.lyy.emotion.mapper.EmotionMapper;
import com.grow.lyy.emotion.service.EmotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmotionServiceImpl implements EmotionService {

    private final EmotionMapper emotionMapper;

    private final DiaryEmotionMapper diaryEmotionMapper;

    private final DiaryMapper diaryMapper;

    @Override
    public void createEmotion(Emotion emotion) {

        if (emotion == null) {
            throw new BusinessException("情绪不能为空");
        }

        if (emotion.getName() == null
                || emotion.getName().isBlank()) {
            throw new BusinessException("情绪名称不能为空");
        }

        Long count = emotionMapper.selectCount(
                new LambdaQueryWrapper<Emotion>()
                        .eq(
                                Emotion::getName,
                                emotion.getName()
                        )
        );

        if (count > 0) {
            throw new BusinessException("情绪已存在");
        }

        emotionMapper.insert(emotion);
    }

    @Override
    public List<Emotion> getAllEmotions() {

        return emotionMapper.selectList(
                new LambdaQueryWrapper<Emotion>()
                        .orderByAsc(Emotion::getId)
        );
    }

    @Override
    public Emotion getById(Long id) {

        Emotion emotion =
                emotionMapper.selectById(id);

        if (emotion == null) {
            throw new BusinessException("情绪不存在");
        }

        return emotion;
    }

    @Override
    public void deleteEmotion(Long id) {

        Emotion emotion = getById(id);

        diaryEmotionMapper.delete(
                new LambdaQueryWrapper<DiaryEmotion>()
                        .eq(
                                DiaryEmotion::getEmotionId,
                                emotion.getId()
                        )
        );

        emotionMapper.deleteById(
                emotion.getId()
        );
    }

    @Override
    public void addDiaryEmotion(
            Long userId,
            Long diaryId,
            Long emotionId
    ) {

        requireDiaryOwnership(userId, diaryId);

        getById(emotionId);

        Long count =
                diaryEmotionMapper.selectCount(
                        new LambdaQueryWrapper<DiaryEmotion>()
                                .eq(
                                        DiaryEmotion::getDiaryId,
                                        diaryId
                                )
                                .eq(
                                        DiaryEmotion::getEmotionId,
                                        emotionId
                                )
                );

        if (count != null && count > 0) {
            throw new BusinessException(
                    "该随笔已经属于这个情绪"
            );
        }

        DiaryEmotion diaryEmotion =
                new DiaryEmotion();

        diaryEmotion.setDiaryId(diaryId);
        diaryEmotion.setEmotionId(emotionId);

        diaryEmotionMapper.insert(
                diaryEmotion
        );
    }

    @Override
    public void removeDiaryEmotion(
            Long userId,
            Long diaryId,
            Long emotionId
    ) {

        requireDiaryOwnership(userId, diaryId);

        int result =
                diaryEmotionMapper.delete(
                        new LambdaQueryWrapper<DiaryEmotion>()
                                .eq(
                                        DiaryEmotion::getDiaryId,
                                        diaryId
                                )
                                .eq(
                                        DiaryEmotion::getEmotionId,
                                        emotionId
                                )
                );

        if (result == 0) {
            throw new BusinessException(
                    "该随笔不属于这个情绪"
            );
        }
    }

    @Override
    public List<Emotion> getDiaryEmotions(
            Long userId,
            Long diaryId
    ) {

        requireDiaryOwnership(userId, diaryId);

        List<DiaryEmotion> relations =
                diaryEmotionMapper.selectList(
                        new LambdaQueryWrapper<DiaryEmotion>()
                                .eq(
                                        DiaryEmotion::getDiaryId,
                                        diaryId
                                )
                );

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> emotionIds = relations.stream()
                .map(DiaryEmotion::getEmotionId)
                .distinct()
                .toList();

        Map<Long, Emotion> emotionsById =
                emotionMapper.selectBatchIds(emotionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Emotion::getId,
                                Function.identity()
                        ));

        return relations.stream()
                .map(relation -> emotionsById.get(relation.getEmotionId()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<DiaryVO> getEmotionDiaries(
            Long userId,
            Long emotionId
    ) {

        requireUserId(userId);

        // 先确认瓶子存在
        getById(emotionId);

        List<DiaryEmotion> relations =
                diaryEmotionMapper.selectList(
                        new LambdaQueryWrapper<DiaryEmotion>()
                                .eq(
                                        DiaryEmotion::getEmotionId,
                                        emotionId
                                )
                                .orderByDesc(
                                        DiaryEmotion::getCreateTime
                                )
                );

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> diaryIds = relations.stream()
                .map(DiaryEmotion::getDiaryId)
                .distinct()
                .toList();

        Map<Long, Diary> diariesById =
                diaryMapper.selectList(
                                new LambdaQueryWrapper<Diary>()
                                        .in(
                                                Diary::getId,
                                                diaryIds
                                        )
                                        .eq(
                                                Diary::getUserId,
                                                userId
                                        )
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                Diary::getId,
                                Function.identity()
                        ));

        return relations.stream()
                .map(relation -> diariesById.get(relation.getDiaryId()))
                .filter(Objects::nonNull)
                .map(this::toDiaryVO)
                .toList();
    }
    @Override
    public Emotion getByName(String name) {

        if(name == null || name.isBlank()){
            return null;
        }


        return emotionMapper.selectOne(
                new LambdaQueryWrapper<Emotion>()
                        .eq(
                                Emotion::getName,
                                name
                        )
        );
    }

    /**
     * 所有随笔相关操作都必须带当前用户，避免通过 diaryId 越权访问。
     */
    private void requireDiaryOwnership(
            Long userId,
            Long diaryId
    ) {

        requireUserId(userId);

        if (diaryId == null) {
            throw new BusinessException("随笔不存在或无权访问");
        }

        Long count = diaryMapper.selectCount(
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

        if (count == null || count == 0) {
            throw new BusinessException("随笔不存在或无权访问");
        }
    }

    private void requireUserId(Long userId) {

        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
    }

    /**
     * Diary 转 DiaryVO
     *
     * 注意：
     * 这里不查询 emotions，
     * 避免出现“瓶子 → 随笔 → 瓶子”的递归查询。
     */
    private DiaryVO toDiaryVO(Diary diary) {

        DiaryVO vo =
                new DiaryVO();

        vo.setId(
                diary.getId()
        );

        vo.setTitle(
                diary.getTitle()
        );

        vo.setContent(
                diary.getContent()
        );

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

        return vo;
    }
}

