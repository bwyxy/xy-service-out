package com.xy.admin.controller;


import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysImUser;
import com.xy.system.service.ISysImUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 悟空 IM系统账号
 * */
@RestController
@RequestMapping("/wk")
@RequiredArgsConstructor
public class SysImUserController {

    @Autowired
    private ISysImUserService sysImUserService;

    /**
     * 获取列表
     * */
    @PostMapping("/system/user")
    public R<PageResult<SysImUser>> selectSysImUserByPage(@RequestBody SysImUser sysImUser) {
        return R.ok(sysImUserService.selectSysImUserList(sysImUser));
    }

    /**
     * 添加 IM系统账号
     * */
    @PostMapping("/system/add")
    public R<?>  insertSysImUser(@RequestBody SysImUser sysImUser) {
        sysImUserService.insertSysImUser(sysImUser);

        return R.ok("添加成功");
    }

    /**
     * 更新 IM系统账号
     * */
    @PutMapping("/system/update")
    public R<?> updateSysImUser(@RequestBody SysImUser sysImUser) {
        sysImUserService.updateSysImUser(sysImUser);

        return R.ok("更新成功");
    }
}
