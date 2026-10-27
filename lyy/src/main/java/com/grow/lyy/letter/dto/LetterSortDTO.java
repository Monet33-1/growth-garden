package com.grow.lyy.letter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class LetterSortDTO {

    @NotNull(message = "信件编号不能为空")
    @Positive(message = "信件编号无效")
    private Long id;

    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不能小于 0")
    private Integer sort;
}
