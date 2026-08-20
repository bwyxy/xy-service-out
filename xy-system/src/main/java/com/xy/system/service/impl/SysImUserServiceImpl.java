package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.client.WuKongClient;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysImUser;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysImUserMapper;
import com.xy.system.service.ISysImUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysImUserServiceImpl extends ServiceImpl<SysImUserMapper, SysImUser>  implements ISysImUserService {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 获取列表
     * */
    @Override
    public PageResult<SysImUser> selectSysImUserList(SysImUser sysImUser) {
        LambdaQueryWrapper<SysImUser> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotEmpty(sysImUser.getNickname())) {
            queryWrapper.eq(SysImUser::getNickname, sysImUser.getNickname());
        }

        if (StringUtils.isNotEmpty(sysImUser.getUserKey())) {
            queryWrapper.eq(SysImUser::getUserKey, sysImUser.getUserKey());
        }

        Page<SysImUser> page = this.page(new Page<>(sysImUser.getPageNum(), sysImUser.getPageSize()), queryWrapper);

        List<SysImUser> list = syncWKIMBindStatus(page.getRecords());

        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 新增 IM系统账号
     * */
    @Override
    public void insertSysImUser(SysImUser sysImUser) {
        save(sysImUser);
    }

    /**
     * 更新 IM系统账号
     * */
    @Override
    public void updateSysImUser(SysImUser sysImUser) {
        if (sysImUser.getId() == null) {
            throw new BusinessException("账号 Id不能为空");
        }

        LambdaUpdateWrapper<SysImUser> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(SysImUser::getId, sysImUser.getId());

        if (StringUtils.isNotEmpty(sysImUser.getNickname())) {
            updateWrapper.set(SysImUser::getNickname, sysImUser.getNickname());
        }
        if (StringUtils.isNotEmpty(sysImUser.getAvatar())) {
            updateWrapper.set(SysImUser::getAvatar, sysImUser.getAvatar());
        }
        if (StringUtils.isNotEmpty(sysImUser.getUserKey())) {
            updateWrapper.set(SysImUser::getUserKey, sysImUser.getUserKey());
        }

        update(updateWrapper);
    }

    /** 同步 悟空IM是否绑定 */
    private List<SysImUser> syncWKIMBindStatus(List<SysImUser> list) {
        List<String> systemUids = wuKongClient.getSystemUids();

        for (SysImUser sysImUser : list) {
            if (StringUtils.isNotEmpty(sysImUser.getWkImKey())) {
                sysImUser.setBindWkImKey(false);
            } else {
                sysImUser.setBindWkImKey(systemUids.contains(sysImUser.getWkImKey()));
            };
        }

        return list;
    }
}
