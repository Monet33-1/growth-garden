package com.grow.lyy.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminLoginDTO {

    @NotBlank(message = "请填写管理员账号")
    private String username;

    @NotBlank(message = "请填写管理员密码")
    @Size(max = 128, message = "管理员密码格式无效")
    private String password;
}
