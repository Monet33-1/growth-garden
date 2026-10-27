package com.grow.lyy.letter.vo;

import lombok.Data;

@Data
public class LetterVO {


    private Long id;


    private String title;


    /**
     * Markdown原文
     */
    private String content;


    /**
     * HTML展示内容
     */
    private String contentHtml;


    private String cover;


    private String status;

    private Long diaryId;

}
