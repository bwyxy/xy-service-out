package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 修改个人资料的入参（只覆盖 t_user 中允许用户自己改的字段）
 *
 * <p>注意：不接受 u_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。
 * 字段为 null 表示本次不改，因此单独改性别时不用把昵称和头像一起带上。</p>
 * */
@Data
public class TodoProfileUpdateDTO {

    /**
     * 昵称
     * */
    @JsonProperty("nickname")
    private String nickname;

    /**
     * 头像 URL
     * */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * 性别 0-未知 1-男 2-女
     * */
    @JsonProperty("sex")
    private String sex;
}
