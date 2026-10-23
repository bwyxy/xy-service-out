package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoHabitCheckDTO;
import com.xy.app.pojo.dto.TodoHabitSaveDTO;
import com.xy.app.pojo.vo.TodoHabitVO;
import com.xy.app.service.IHabitService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端习惯打卡
 * */
@RestController
@RequestMapping("/habit")
@RequiredArgsConstructor
public class HabitController {

    private final IHabitService habitService;

    /**
     * 习惯列表（含连续打卡天数与本周打卡情况）
     * */
    @GetMapping("/list")
    public R<List<TodoHabitVO>> list() {
        return R.ok(habitService.listHabits());
    }

    /**
     * 新增习惯
     * */
    @PostMapping("/create")
    public R<TodoHabitVO> create(@RequestBody TodoHabitSaveDTO dto) {
        return R.ok(habitService.createHabit(dto));
    }

    /**
     * 修改习惯
     * */
    @PutMapping("/{habitId}")
    public R<Void> update(@PathVariable Long habitId, @RequestBody TodoHabitSaveDTO dto) {
        habitService.updateHabit(habitId, dto);

        return R.ok();
    }

    /**
     * 删除习惯（历史打卡记录保留）
     * */
    @DeleteMapping("/{habitId}")
    public R<Void> remove(@PathVariable Long habitId) {
        habitService.removeHabit(habitId);

        return R.ok();
    }

    /**
     * 打卡 / 取消打卡（同一天再调一次即取消），返回该习惯最新状态
     * */
    @PostMapping("/{habitId}/check")
    public R<TodoHabitVO> check(@PathVariable Long habitId,
                                @RequestBody(required = false) TodoHabitCheckDTO dto) {
        return R.ok(habitService.checkHabit(habitId, dto));
    }
}
