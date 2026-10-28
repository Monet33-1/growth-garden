
package com.grow.lyy.diary.vo;

import com.grow.lyy.emotion.entity.Emotion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiaryVO {

    private Long id;

    private String title;

    /**
     * Markdown原文
     */
    private String content;

    /**
     * Markdown解析后的HTML
     */
    private String contentHtml;

    private String mood;

    private LocalDateTime createTime;

    /**
     * 这篇随笔所属的情绪瓶子
     */
    private List<Emotion> emotions;

    /**
     * AI 不可用时为 true，前端应让用户手动选择情绪。
     */
    private boolean emotionFallbackRequired;

}
