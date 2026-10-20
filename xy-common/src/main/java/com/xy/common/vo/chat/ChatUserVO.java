package com.xy.common.vo.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客户端用户列表/详情展示对象
 * */
@Data
public class ChatUserVO {

    /** 用户 UID
     *  Jackson 从 getUId() 推断出的名字是 uid，读写方法需显式声明为 uId */
    @Getter(onMethod_ = @JsonProperty("uId"))
    @Setter(onMethod_ = @JsonProperty("uId"))
    private Long uId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 用户状态 0-正常 1-禁用 */
    private String status;

    /** 性别 0-未知 1-男 2-女 */
    private String sex;

    /** 是否绑定聊天应用 0-未绑定 1-已绑定 */
    private String isBindChat;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 在线状态 1-在线 0-离线 */
    private String online;

    /** 当前在线设备数 */
    private Integer onlineDeviceCount;
}
