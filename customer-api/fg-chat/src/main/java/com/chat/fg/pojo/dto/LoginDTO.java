package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 登录请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {

    /** 手机号或邮箱 */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    /** 设备类型分组：1=PC端 2=平板端 3=手机端（客户端上报，缺省时按 User-Agent 推断） */
    private Integer deviceType;

    /** 设备唯一标识（客户端本地持久化的设备指纹） */
    private String deviceId;
}