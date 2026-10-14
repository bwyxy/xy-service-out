package com.xy.common.enums;

import lombok.Getter;

/**
 * 消息类型
 * */
@Getter
public enum MessageType {

    text(1, "文本"),

    image(2, "图片"),

    audio(3, "语音"),

    file(4, "文件"),

    location(5, "位置"),

    card(6, "名片"),

    newMember(7, "新成员通知"),

    groupAnnouncement(10, "群公告"),

    rtcInvite(100, "音视频邀请"),

    readReceipt(101, "已读回执"),

    recall(102, "消息撤回"),

    rtcAccept(103, "接听"),

    rtcReject(104, "拒绝"),

    rtcHangup(105, "挂断"),

    rtcBusy(106, "忙线"),

    /** RTC 信令 wire 类型（200+，避开前端已占用的 1-103） */

    rtcSignalInvite(200, "RTC呼叫邀请"),

    rtcSignalCancel(201, "RTC主叫取消"),

    rtcSignalAccept(202, "RTC被叫接听"),

    rtcSignalReject(203, "RTC被叫拒绝"),

    rtcSignalHangup(204, "RTC挂断"),

    rtcSignalBusy(205, "RTC忙线"),

    rtcSignalOffer(206, "RTC Offer"),

    rtcSignalAnswer(207, "RTC Answer"),

    rtcSignalCandidate(208, "RTC ICE Candidate"),

    rtcRecord(210, "RTC通话记录");

    private final Integer code;

    private final String msg;
    MessageType(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
