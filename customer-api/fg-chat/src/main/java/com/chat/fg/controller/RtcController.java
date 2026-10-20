package com.chat.fg.controller;

import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.dto.InviteDTO;
import com.chat.fg.pojo.dto.RoomDTO;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.InviteVO;
import com.chat.fg.pojo.vo.RoomDetailVO;
import com.chat.fg.service.IRtcService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 音视频通话 控制层
 * */
@RestController
@RequestMapping("/rtc")
@RequiredArgsConstructor
public class RtcController {

    private final IRtcService rtcService;

    /**
     * 发起通话
     * */
    @PostMapping("/call")
    public R<CallVO> call(@Valid @RequestBody CallDTO dto) {
        return R.ok(rtcService.call(SecurityUtils.getUId(), dto));
    }

    /**
     * 接听
     * */
    @PostMapping("/accept")
    public R<CallVO> accept(@Valid @RequestBody RoomDTO dto) {
        return R.ok(rtcService.accept(SecurityUtils.getUId(), dto.getRoomId()));
    }

    /**
     * 拒绝
     * */
    @PostMapping("/reject")
    public R<?> reject(@Valid @RequestBody RoomDTO dto) {
        rtcService.reject(SecurityUtils.getUId(), dto.getRoomId());
        return R.ok();
    }

    /**
     * 挂断
     * */
    @PostMapping("/hangup")
    public R<?> hangup(@Valid @RequestBody RoomDTO dto) {
        rtcService.hangup(SecurityUtils.getUId(), dto.getRoomId());
        return R.ok();
    }

    /**
     * 主叫取消
     * */
    @PostMapping("/cancel")
    public R<?> cancel(@Valid @RequestBody RoomDTO dto) {
        rtcService.cancel(SecurityUtils.getUId(), dto.getRoomId());
        return R.ok();
    }

    /**
     * 群通话中追加邀请成员
     * */
    @PostMapping("/invite")
    public R<InviteVO> invite(@Valid @RequestBody InviteDTO dto) {
        return R.ok(rtcService.invite(SecurityUtils.getUId(), dto));
    }

    /**
     * 结束整场通话（发起人或群主/管理员）
     * */
    @PostMapping("/end")
    public R<?> end(@Valid @RequestBody RoomDTO dto) {
        rtcService.end(SecurityUtils.getUId(), dto.getRoomId());
        return R.ok();
    }

    /**
     * 重新签发接入 token（断线重连/刷新页面后重新入房）
     * */
    @PostMapping("/token")
    public R<CallVO> token(@Valid @RequestBody RoomDTO dto) {
        return R.ok(rtcService.token(SecurityUtils.getUId(), dto.getRoomId()));
    }

    /**
     * 查询房间详情
     * */
    @GetMapping("/room/{roomId}")
    public R<RoomDetailVO> roomDetail(@PathVariable String roomId) {
        return R.ok(rtcService.roomDetail(SecurityUtils.getUId(), roomId));
    }

    /**
     * 通话服务健康检查（媒体面 LiveKit 连通性）
     * */
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of("livekit", rtcService.isLivekitUp() ? "up" : "down"));
    }
}
