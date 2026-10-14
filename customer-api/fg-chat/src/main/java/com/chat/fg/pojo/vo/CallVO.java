package com.chat.fg.pojo.vo;

import lombok.Data;

/**
 * 发起通话结果 VO
 */
@Data
public class CallVO {

    /**
     * 房间 ID
     */
    private String roomId;

    /**
     * 媒体类型 1-语音 2-视频
     */
    private Integer mediaType;
}
