package com.chat.fg.config;

import com.chat.fg.pojo.vo.IceServerVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RTC 通话配置
 * */
@Data
@Component
@ConfigurationProperties(prefix = "xy.rtc")
public class RtcProperties {

    /**
     * ICE 服务器列表（STUN/TURN），为空时使用默认公共 STUN
     * */
    private List<IceServerVO> iceServers = new ArrayList<>();

    /**
     * 呼叫超时时间（秒）：超时未接听自动结束房间并通知双方
     * */
    private long callTimeoutSeconds = 90;
}
