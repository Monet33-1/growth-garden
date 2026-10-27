package com.grow.lyy.letter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LetterAdminDTO {

    @NotBlank(message = "请填写信件标题")
    @Size(max = 200, message = "标题不能超过 200 个字符")
    private String title;

    @NotBlank(message = "请填写信件正文")
    @Size(max = 100000, message = "正文内容过长")
    private String content;

    @NotBlank(message = "请选择解锁类型")
    @Pattern(regexp = "DIARY_COUNT", message = "目前仅支持按随笔数量解锁")
    private String unlockType;

    @NotNull(message = "请填写解锁所需随笔数量")
    @Min(value = 0, message = "解锁数量不能小于 0")
    private Integer unlockValue;

    @Size(max = 1000, message = "封面路径不能超过 1000 个字符")
    private String cover;

    @NotNull(message = "请填写排序值")
    @Min(value = 0, message = "排序值不能小于 0")
    private Integer sort;
}
