package com.grow.lyy.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordDTO {
    @NotBlank(message = "请填写当前密码。")
    private String currentPassword;

    @NotBlank(message = "请填写新密码。")
    @Size(min = 8, max = 72, message = "新密码长度需为 8 至 72 个字符。")
    private String newPassword;

    @NotBlank(message = "请再次填写新密码。")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
