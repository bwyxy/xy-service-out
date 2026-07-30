package com.xy.common.client.dto.request;

import com.xy.common.client.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设置会话未读数
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetUnread extends BaseEntity {

    /**
     * 要设置的未读数量
     * */
    private Integer unread;
}
