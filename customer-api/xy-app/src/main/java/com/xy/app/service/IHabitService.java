package com.xy.app.service;

import com.xy.app.pojo.dto.TodoHabitCheckDTO;
import com.xy.app.pojo.dto.TodoHabitSaveDTO;
import com.xy.app.pojo.vo.TodoHabitVO;

import java.util.List;

/**
 * 客户端习惯服务
 * */
public interface IHabitService {

    /**
     * 习惯列表（含连续打卡天数与本周打卡情况）
     * */
    List<TodoHabitVO> listHabits();

    /**
     * 新增习惯
     * */
    TodoHabitVO createHabit(TodoHabitSaveDTO dto);

    /**
     * 修改习惯
     * */
    void updateHabit(Long habitId, TodoHabitSaveDTO dto);

    /**
     * 删除习惯（逻辑删除，打卡记录保留）
     * */
    void removeHabit(Long habitId);

    /**
     * 打卡 / 取消打卡（同一习惯同一天重复调用即取消），返回该习惯最新状态
     * */
    TodoHabitVO checkHabit(Long habitId, TodoHabitCheckDTO dto);
}
