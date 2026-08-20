package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码
 * */
@Data
public class EditPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmNewPassword;

    public boolean isNewAndConfirmNewPassword() {

        if (this.newPassword == null || this.confirmNewPassword == null) {
            return false;
        }

        return this.newPassword.equals(this.confirmNewPassword);
    }
}
