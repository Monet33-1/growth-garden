package com.grow.lyy.diary.vo;

import lombok.Data;

@Data
public class RandomMemoryVO {

    private DiaryVO diary;

    /**
     * 当前筛选条件下已经抽完一轮，这次从池子开头重新开始。
     */
    private boolean poolReset;

    /**
     * 抽完后还剩多少篇未看过的随笔。
     */
    private int remaining;
}
