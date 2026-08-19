package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ISysDictDataService extends IService<SysDictData> {

    /**
     * 分页查询字典数据列表
     * */
    PageResult<SysDictData> selectDictDataList(SysDictData dictData);

    /**
     * 根据字典类型查询字典数据
     * */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 根据字典编码查询详情
     * */
    SysDictData selectDictDataById(Long dictCode);

    /**
     * 新增字典数据
     * */
    Long insertDictData(SysDictData dictData);

    /**
     * 修改字典数据
     * */
    boolean updateDictData(SysDictData dictData);

    /**
     * 删除字典数据
     * */
    boolean deleteDictDataById(Long dictCode);

    /**
     * 导出字典数据
     * */
    void exportDictData(SysDictData dictData, HttpServletResponse response);

    /**
     * 导入字典数据
     * @return 导入结果信息
     * */
    String importDictData(MultipartFile file);
}
