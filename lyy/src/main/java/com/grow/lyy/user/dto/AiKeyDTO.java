package com.grow.lyy.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKeyDTO {

    @Size(max = 200, message = "DeepSeek Key 过长。")
    private String apiKey;
}
