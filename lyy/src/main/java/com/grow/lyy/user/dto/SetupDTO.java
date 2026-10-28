package com.grow.lyy.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetupDTO {

    @NotBlank(message = "请填写用户名。")
    @Size(max = 50, message = "用户名不能超过 50 个字符。")
    private String username;

    @NotBlank(message = "请填写密码。")
    @Size(min = 8, max = 72, message = "密码长度需为 8 至 72 个字符。")
    private String password;

    @Size(max = 50, message = "昵称不能超过 50 个字符。")
    private String nickname;

    @Size(max = 200, message = "DeepSeek Key 过长。")
    private String deepseekApiKey;
}
