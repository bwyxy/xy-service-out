package com.xy.app.service;

import com.xy.app.pojo.dto.TodoFocusFinishDTO;
import com.xy.app.pojo.dto.TodoFocusStartDTO;
import com.xy.app.pojo.vo.TodoFocusTodayVO;

/**
 * 客户端番茄专注服务
 * */
public interface IFocusService {

    /**
     * 今日专注统计（顺带把进程意外中断遗留的 running 记录收成 aborted）
     * */
    TodoFocusTodayVO today();

    /**
     * 开始一轮专注（clientId 幂等）
     * */
    void start(TodoFocusStartDTO dto);

    /**
     * 结束一轮专注：跑满一轮传 finished，中途放弃传 aborted
     * */
    void finish(TodoFocusFinishDTO dto);
}
