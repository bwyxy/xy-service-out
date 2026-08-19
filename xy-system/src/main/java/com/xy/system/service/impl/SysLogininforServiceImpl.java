package com.xy.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysLogininfor;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysLogininforMapper;
import com.xy.system.service.ISysLogininforService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 登录日志服务实现
 * */
@Slf4j
@Service
public class SysLogininforServiceImpl extends ServiceImpl<SysLogininforMapper, SysLogininfor> implements ISysLogininforService {

    @Override
    public PageResult<SysLogininfor> selectLogininforPage(SysLogininfor logininfor) {
        int pageNum = logininfor.getPageNum() <= 0 ? 1 : logininfor.getPageNum();
        int pageSize = logininfor.getPageSize() <= 0 ? 10 : logininfor.getPageSize();

        LambdaQueryWrapper<SysLogininfor> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(logininfor.getUserName())) {
            queryWrapper.like(SysLogininfor::getUserName, logininfor.getUserName());
        }
        if (StringUtils.isNotEmpty(logininfor.getIpaddr())) {
            queryWrapper.like(SysLogininfor::getIpaddr, logininfor.getIpaddr());
        }
        if (StringUtils.isNotEmpty(logininfor.getStatus())) {
            queryWrapper.eq(SysLogininfor::getStatus, logininfor.getStatus());
        }

        Map<String, Object> params = logininfor.getParams();
        if (params != null) {
            String beginTime = (String) params.get("beginTime");
            String endTime = (String) params.get("endTime");
            if (StringUtils.isNotEmpty(beginTime)) {
                queryWrapper.ge(SysLogininfor::getLoginTime, parseTime(beginTime));
            }
            if (StringUtils.isNotEmpty(endTime)) {
                queryWrapper.le(SysLogininfor::getLoginTime, parseTime(endTime));
            }
        }

        queryWrapper.orderByDesc(SysLogininfor::getLoginTime);

        Page<SysLogininfor> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void deleteLogininforById(Long infoId) {
        getBaseMapper().deleteById(infoId);
    }

    @Override
    public void cleanLogininfor() {
        this.remove(new LambdaQueryWrapper<>());
    }

    @Override
    public void exportLogininfor(SysLogininfor logininfor, HttpServletResponse response) {
        List<SysLogininfor> list = this.list(this.buildQueryWrapper(logininfor));

        try {
            String fileName = URLEncoder.encode("登录日志_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), SysLogininfor.class)
                    .sheet("登录日志")
                    .doWrite(list);
        } catch (IOException e) {
            log.error("导出登录日志失败", e);
            throw new RuntimeException("导出登录日志失败", e);
        }
    }

    private LambdaQueryWrapper<SysLogininfor> buildQueryWrapper(SysLogininfor logininfor) {
        LambdaQueryWrapper<SysLogininfor> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(logininfor.getUserName())) {
            queryWrapper.like(SysLogininfor::getUserName, logininfor.getUserName());
        }
        if (StringUtils.isNotEmpty(logininfor.getIpaddr())) {
            queryWrapper.like(SysLogininfor::getIpaddr, logininfor.getIpaddr());
        }
        if (StringUtils.isNotEmpty(logininfor.getStatus())) {
            queryWrapper.eq(SysLogininfor::getStatus, logininfor.getStatus());
        }

        Map<String, Object> params = logininfor.getParams();
        if (params != null) {
            String beginTime = (String) params.get("beginTime");
            String endTime = (String) params.get("endTime");
            if (StringUtils.isNotEmpty(beginTime)) {
                queryWrapper.ge(SysLogininfor::getLoginTime, parseTime(beginTime));
            }
            if (StringUtils.isNotEmpty(endTime)) {
                queryWrapper.le(SysLogininfor::getLoginTime, parseTime(endTime));
            }
        }

        queryWrapper.orderByDesc(SysLogininfor::getLoginTime);
        return queryWrapper;
    }

    private LocalDateTime parseTime(String time) {
        return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
