package com.chat.fg.controller;

import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.dto.RoomDTO;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.IceServerVO;
import com.chat.fg.service.IRtcService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public R<?> accept(@Valid @RequestBody RoomDTO dto) {
        rtcService.accept(SecurityUtils.getUId(), dto.getRoomId());
        return R.ok();
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
     * 获取 ICE 服务器配置
     * */
    @GetMapping("/ice-servers")
    public R<List<IceServerVO>> getIceServers() {
        return R.ok(rtcService.getIceServers());
    }
}
