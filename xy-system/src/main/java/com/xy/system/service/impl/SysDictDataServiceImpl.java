package com.xy.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictData;
import com.xy.common.entity.SysDictType;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysDictDataMapper;
import com.xy.system.service.ISysDictDataService;
import com.xy.system.service.ISysDictTypeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 字典数据服务实现
 * */
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    private final ISysDictTypeService dictTypeService;

    @Override
    public PageResult<SysDictData> selectDictDataList(SysDictData dictData) {
        int pageNum = dictData.getPageNum() <= 0 ? 1 : dictData.getPageNum();
        int pageSize = dictData.getPageSize() <= 0 ? 10 : dictData.getPageSize();

        LambdaQueryWrapper<SysDictData> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dictData.getDictType())) {
            queryWrapper.eq(SysDictData::getDictType, dictData.getDictType());
        }
        if (StringUtils.isNotEmpty(dictData.getDictLabel())) {
            queryWrapper.like(SysDictData::getDictLabel, dictData.getDictLabel());
        }
        if (StringUtils.isNotEmpty(dictData.getDictValue())) {
            queryWrapper.like(SysDictData::getDictValue, dictData.getDictValue());
        }
        if (StringUtils.isNotEmpty(dictData.getStatus())) {
            queryWrapper.eq(SysDictData::getStatus, dictData.getStatus());
        }
        queryWrapper.orderByAsc(SysDictData::getDictSort);

        Page<SysDictData> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Cacheable(value = "dict", key = "#dictType")
    public List<SysDictData> selectDictDataByType(String dictType) {
        LambdaQueryWrapper<SysDictData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort);
        return list(queryWrapper);
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return getById(dictCode);
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public Long insertDictData(SysDictData dictData) {
        validateDictType(dictData.getDictType());
        fillDefaultValues(dictData);
        boolean success = save(dictData);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "新增字典数据失败");
        }
        return dictData.getDictCode();
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public boolean updateDictData(SysDictData dictData) {
        SysDictData existing = getById(dictData.getDictCode());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "字典数据不存在");
        }
        validateDictType(dictData.getDictType());
        fillDefaultValues(dictData);
        boolean success = updateById(dictData);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "修改字典数据失败");
        }
        return true;
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public boolean deleteDictDataById(Long dictCode) {
        SysDictData existing = getById(dictCode);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "字典数据不存在");
        }
        return getBaseMapper().deleteById(dictCode) > 0;
    }

    @Override
    public void exportDictData(SysDictData dictData, HttpServletResponse response) {
        LambdaQueryWrapper<SysDictData> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dictData.getDictType())) {
            queryWrapper.eq(SysDictData::getDictType, dictData.getDictType());
        }
        if (StringUtils.isNotEmpty(dictData.getDictLabel())) {
            queryWrapper.like(SysDictData::getDictLabel, dictData.getDictLabel());
        }
        if (StringUtils.isNotEmpty(dictData.getDictValue())) {
            queryWrapper.like(SysDictData::getDictValue, dictData.getDictValue());
        }
        if (StringUtils.isNotEmpty(dictData.getStatus())) {
            queryWrapper.eq(SysDictData::getStatus, dictData.getStatus());
        }
        queryWrapper.orderByAsc(SysDictData::getDictSort);
        List<SysDictData> list = list(queryWrapper);

        List<DictDataExcel> excelList = new ArrayList<>();
        for (SysDictData item : list) {
            DictDataExcel excel = new DictDataExcel();
            excel.setDictSort(item.getDictSort());
            excel.setDictLabel(item.getDictLabel());
            excel.setDictValue(item.getDictValue());
            excel.setDictType(item.getDictType());
            excel.setCssClass(item.getCssClass());
            excel.setListClass(item.getListClass());
            excel.setIsDefault(item.getIsDefault());
            excel.setStatus(item.getStatus());
            excelList.add(excel);
        }

        try {
            String fileName = URLEncoder.encode("字典数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), DictDataExcel.class)
                    .sheet("字典数据")
                    .doWrite(excelList);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_DOWNLOAD_ERROR, "导出字典数据失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importDictData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "导入文件不能为空");
        }

        List<DictDataExcel> errorList = new ArrayList<>();
        List<SysDictData> successList = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), DictDataExcel.class, new ReadListener<DictDataExcel>() {
                @Override
                public void invoke(DictDataExcel excel, AnalysisContext context) {
                    Integer rowIndex = context.readRowHolder().getRowIndex();
                    if (StringUtils.isEmpty(excel.getDictType()) || StringUtils.isEmpty(excel.getDictLabel())
                            || StringUtils.isEmpty(excel.getDictValue())) {
                        errorList.add(excel);
                        return;
                    }

                    SysDictType type = dictTypeService.lambdaQuery().eq(SysDictType::getDictType, excel.getDictType()).one();
                    if (type == null) {
                        errorList.add(excel);
                        return;
                    }

                    SysDictData data = new SysDictData();
                    data.setDictSort(excel.getDictSort() == null ? 0 : excel.getDictSort());
                    data.setDictLabel(excel.getDictLabel());
                    data.setDictValue(excel.getDictValue());
                    data.setDictType(excel.getDictType());
                    data.setCssClass(excel.getCssClass());
                    data.setListClass(excel.getListClass());
                    data.setIsDefault(StringUtils.isEmpty(excel.getIsDefault()) ? "N" : excel.getIsDefault());
                    data.setStatus(StringUtils.isEmpty(excel.getStatus()) ? "0" : excel.getStatus());
                    successList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "读取导入文件失败");
        }

        int successCount = 0;
        int updateCount = 0;
        for (SysDictData data : successList) {
            SysDictData existing = lambdaQuery()
                    .eq(SysDictData::getDictType, data.getDictType())
                    .eq(SysDictData::getDictValue, data.getDictValue())
                    .one();
            if (existing != null) {
                data.setDictCode(existing.getDictCode());
                if (updateById(data)) {
                    updateCount++;
                }
            } else {
                if (save(data)) {
                    successCount++;
                }
            }
        }

        return String.format("导入完成：新增 %d 条，更新 %d 条，失败 %d 条", successCount, updateCount, errorList.size());
    }

    private void validateDictType(String dictType) {
        if (StringUtils.isEmpty(dictType)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "字典类型不能为空");
        }
        SysDictType type = dictTypeService.lambdaQuery().eq(SysDictType::getDictType, dictType).one();
        if (type == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "字典类型不存在");
        }
    }

    private void fillDefaultValues(SysDictData dictData) {
        if (dictData.getDictSort() == null) {
            dictData.setDictSort(0);
        }
        if (StringUtils.isEmpty(dictData.getStatus())) {
            dictData.setStatus("0");
        }
        if (StringUtils.isEmpty(dictData.getIsDefault())) {
            dictData.setIsDefault("N");
        }
    }

    /**
     * 字典数据导出/导入模型
     * */
    @Data
    public static class DictDataExcel {
        @ExcelProperty("字典排序")
        private Integer dictSort;

        @ExcelProperty("字典标签")
        private String dictLabel;

        @ExcelProperty("字典键值")
        private String dictValue;

        @ExcelProperty("字典类型")
        private String dictType;

        @ExcelProperty("样式属性")
        private String cssClass;

        @ExcelProperty("表格回显样式")
        private String listClass;

        @ExcelProperty("是否默认")
        private String isDefault;

        @ExcelProperty("状态")
        private String status;
    }
}
