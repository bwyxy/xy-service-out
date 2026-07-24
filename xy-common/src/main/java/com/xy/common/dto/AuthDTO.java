package com.xy.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthDTO {

    /**
     * 账号
     * */
    @NotNull(message = "用户名不能为 null")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在 2-50 之间")
    @JsonProperty("username")
    private String username;

    /**
     * 密码
     * */
    @NotNull(message = "密码不能为 null")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 之间")
    @JsonProperty("password")
    private String password;

    /**
     * 自定义验证方法（可选，用于更复杂的验证逻辑）
     */
    public void validate() {
        if (username == null) {
            throw new IllegalArgumentException("用户名不能为 null");
        }
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null) {
            throw new IllegalArgumentException("密码不能为 null");
        }
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }
}
