package com.chat.fg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RTC 通话配置
 * */
@Data
@Component
@ConfigurationProperties(prefix = "xy.rtc")
public class RtcProperties {

    /**
     * 呼叫超时时间（秒）：超时未接听自动结束房间并通知双方
     * */
    private long callTimeoutSeconds = 90;

    /**
     * 通话人数上限（1v1 固定 2 人，群聊取该值）
     * */
    private int maxParticipants = 9;

    /**
     * 单次通话最长时长（分钟），超时由定时任务兜底结束
     * */
    private int maxCallMinutes = 180;

    /**
     * LiveKit SFU 配置
     * */
    private Livekit livekit = new Livekit();

    @Data
    public static class Livekit {

        /**
         * 下发给前端的 LiveKit 地址（ws:// 或 wss://）
         * */
        private String url = "ws://127.0.0.1:7880";

        /**
         * 服务端 RoomService 调用地址（http:// 或 https://，通常为内网）
         * */
        private String apiUrl = "http://127.0.0.1:7880";

        /**
         * API Key
         * */
        private String apiKey;

        /**
         * API Secret
         * */
        private String apiSecret;

        /**
         * 接入 token 有效期（秒），需覆盖断线重连
         * */
        private long tokenTtlSeconds = 14400;

        /**
         * 房间空置自动关闭时间（秒）
         * */
        private int emptyTimeoutSeconds = 300;

        /**
         * 参与者掉线宽限时间（秒）
         * */
        private int departureTimeoutSeconds = 20;
    }
}
