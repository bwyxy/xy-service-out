package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictType;

public interface ISysDictTypeService extends IService<SysDictType> {

    /**
     * 分页查询字典类型列表
     * */
    PageResult<SysDictType> selectDictTypeList(SysDictType dictType);

    /**
     * 根据字典主键查询详情
     * */
    SysDictType selectDictTypeById(Long dictId);

    /**
     * 新增字典类型
     * */
    Long insertDictType(SysDictType dictType);

    /**
     * 修改字典类型
     * */
    boolean updateDictType(SysDictType dictType);

    /**
     * 删除字典类型
     * */
    boolean deleteDictTypeById(Long dictId);

    /**
     * 校验字典类型是否唯一
     * */
    boolean checkDictTypeUnique(SysDictType dictType);
}
