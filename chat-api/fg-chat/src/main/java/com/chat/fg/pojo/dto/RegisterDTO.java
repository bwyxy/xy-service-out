package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 注册用户
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {

    private String nickname;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    @NotBlank(message = "重复密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    private String sex;

    /**
     * 自定义校验逻辑: 密码与重复密码不一致
     * */
    public boolean isPasswordConfirmPassword() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}
