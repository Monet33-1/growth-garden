
package com.grow.lyy.diary.service;

import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.vo.DiaryVO;
import com.grow.lyy.diary.vo.RandomMemoryVO;

import java.util.List;

public interface DiaryService {

    /**
     * 创建随笔
     */
    DiaryVO createDiary(Long userId, Diary diary);

    /**
     * 查询自己的随笔
     */
    List<DiaryVO> getMyDiary(Long userId);

    /**
     * 随机抽取一篇用户随笔，尽量避开已经看过的。
     */
    RandomMemoryVO getRandomMemory(Long userId, List<Long> excludeIds, Long emotionId);

    /**
     * 查询用户随笔数量
     */
    long countDiary(Long userId);

    /**
     * 查询随笔详情
     */
    DiaryVO getDiaryDetail(Long userId, Long diaryId);

    /**
     * 修改随笔
     */
    void updateDiary(Long userId, Long diaryId, Diary diary);

    /**
     * 删除随笔，同时清理情绪关联与信件完成记录
     */
    void deleteDiary(Long userId, Long diaryId);
}

