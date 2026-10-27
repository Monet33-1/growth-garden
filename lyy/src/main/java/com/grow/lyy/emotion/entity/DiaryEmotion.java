
package com.grow.lyy.emotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diary_emotion")
public class DiaryEmotion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long diaryId;

    private Long emotionId;

    private LocalDateTime createTime;

}
