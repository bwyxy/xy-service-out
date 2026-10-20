package com.xy.common.vo.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户黑名单展示对象（附带被拉黑方信息，便于后台列表直接展示）
 * */
@Data
public class UserBlacklistVO {

    /** 主键 */
    private Long id;

    /** 拉黑发起方 UID
     *  Jackson 从 getUId() 推断出的名字是 uid，读写方法需显式声明为 uId */
    @Getter(onMethod_ = @JsonProperty("uId"))
    @Setter(onMethod_ = @JsonProperty("uId"))
    private Long uId;

    /** 拉黑发起方昵称 */
    private String nickname;

    /** 被拉黑用户 UID */
    private Long targetUid;

    /** 被拉黑用户昵称 */
    private String targetNickname;

    /** 被拉黑用户手机号 */
    private String targetPhone;

    /** 备注 */
    private String remark;

    /** 操作人 */
    private String operator;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
