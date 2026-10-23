package com.xy.app.service;

import com.xy.app.pojo.dto.TodoTemplateApplyDTO;
import com.xy.app.pojo.vo.TodoTemplateApplyVO;
import com.xy.app.pojo.vo.TodoTemplateVO;

import java.util.List;

/**
 * 客户端清单模板服务
 * */
public interface ITemplateService {

    /**
     * 模板列表（只返回上架的，按 sort_no 升序）
     * */
    List<TodoTemplateVO> listTemplates();

    /**
     * 套用模板：新建一个清单 + 按模板预置任务（按 batchId 幂等）
     *
     * @param code 模板编码
     * */
    TodoTemplateApplyVO applyTemplate(String code, TodoTemplateApplyDTO dto);
}
