
package com.grow.lyy.diary.service;

import com.grow.lyy.diary.entity.Diary;
import com.grow.lyy.diary.vo.DiaryVO;

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

