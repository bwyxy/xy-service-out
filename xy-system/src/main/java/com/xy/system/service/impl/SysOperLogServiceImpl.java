package com.xy.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysOperLog;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysOperLogMapper;
import com.xy.system.service.ISysOperLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现
 * */
@Service
@Slf4j
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    @Override
    public PageResult<SysOperLog> selectOperLogPage(SysOperLog operLog) {
        int pageNum = operLog.getPageNum() <= 0 ? 1 : operLog.getPageNum();
        int pageSize = operLog.getPageSize() <= 0 ? 10 : operLog.getPageSize();

        LambdaQueryWrapper<SysOperLog> queryWrapper = buildQueryWrapper(operLog);
        queryWrapper.orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void cleanOperLog() {
        this.remove(new LambdaQueryWrapper<>());
    }

    @Override
    public void exportOperLog(SysOperLog operLog, HttpServletResponse response) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = buildQueryWrapper(operLog);
        queryWrapper.orderByDesc(SysOperLog::getOperTime);
        List<SysOperLog> list = list(queryWrapper);

        List<OperLogExcel> excelList = new ArrayList<>();
        for (SysOperLog item : list) {
            OperLogExcel excel = new OperLogExcel();
            excel.setTitle(item.getTitle());
            excel.setBusinessType(formatBusinessType(item.getBusinessType()));
            excel.setRequestMethod(item.getRequestMethod());
            excel.setOperName(item.getOperName());
            excel.setOperUrl(item.getOperUrl());
            excel.setOperIp(item.getOperIp());
            excel.setOperLocation(item.getOperLocation());
            excel.setStatus(formatStatus(item.getStatus()));
            excel.setOperTime(item.getOperTime());
            excel.setCostTime(item.getCostTime());
            excel.setErrorMsg(item.getErrorMsg());
            excelList.add(excel);
        }

        try {
            String fileName = URLEncoder.encode("操作日志_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), OperLogExcel.class)
                    .sheet("操作日志")
                    .doWrite(excelList);
        } catch (IOException e) {
            log.error("导出操作日志失败", e);
            throw new BusinessException(ResultCode.FILE_DOWNLOAD_ERROR, "导出操作日志失败");
        }
    }

    private LambdaQueryWrapper<SysOperLog> buildQueryWrapper(SysOperLog operLog) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(operLog.getTitle())) {
            queryWrapper.like(SysOperLog::getTitle, operLog.getTitle());
        }
        if (StringUtils.isNotEmpty(operLog.getOperName())) {
            queryWrapper.like(SysOperLog::getOperName, operLog.getOperName());
        }
        if (operLog.getBusinessType() != null) {
            queryWrapper.eq(SysOperLog::getBusinessType, operLog.getBusinessType());
        }
        if (operLog.getStatus() != null) {
            queryWrapper.eq(SysOperLog::getStatus, operLog.getStatus());
        }

        Map<String, Object> params = operLog.getParams();
        if (params != null) {
            String beginTime = (String) params.get("beginTime");
            String endTime = (String) params.get("endTime");
            if (StringUtils.isNotEmpty(beginTime)) {
                queryWrapper.ge(SysOperLog::getOperTime, parseTime(beginTime));
            }
            if (StringUtils.isNotEmpty(endTime)) {
                queryWrapper.le(SysOperLog::getOperTime, parseTime(endTime));
            }
        }
        return queryWrapper;
    }

    private LocalDateTime parseTime(String time) {
        return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatBusinessType(Integer businessType) {
        if (businessType == null) {
            return "其它";
        }
        return switch (businessType) {
            case 1 -> "新增";
            case 2 -> "修改";
            case 3 -> "删除";
            case 4 -> "授权";
            case 5 -> "导出";
            case 6 -> "导入";
            case 7 -> "强退";
            case 8 -> "生成代码";
            case 9 -> "清空数据";
            default -> "其它";
        };
    }

    private String formatStatus(Integer status) {
        if (status == null) {
            return "正常";
        }
        return status == 0 ? "正常" : "异常";
    }

    @Data
    public static class OperLogExcel {
        @ExcelProperty("操作模块")
        private String title;

        @ExcelProperty("操作类型")
        private String businessType;

        @ExcelProperty("请求方式")
        private String requestMethod;

        @ExcelProperty("操作人员")
        private String operName;

        @ExcelProperty("请求URL")
        private String operUrl;

        @ExcelProperty("主机地址")
        private String operIp;

        @ExcelProperty("操作地点")
        private String operLocation;

        @ExcelProperty("操作状态")
        private String status;

        @ExcelProperty("操作时间")
        private LocalDateTime operTime;

        @ExcelProperty("消耗时间(ms)")
        private Long costTime;

        @ExcelProperty("错误消息")
        private String errorMsg;
    }
}
