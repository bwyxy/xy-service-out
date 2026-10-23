package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoTemplateApplyDTO;
import com.xy.app.pojo.vo.TodoTemplateApplyVO;
import com.xy.app.pojo.vo.TodoTemplateVO;
import com.xy.app.service.ITemplateService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端待办清单模板
 * */
@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class TemplateController {

    private final ITemplateService templateService;

    /**
     * 模板列表
     * */
    @GetMapping("/list")
    public R<List<TodoTemplateVO>> list() {
        return R.ok(templateService.listTemplates());
    }

    /**
     * 套用模板：新建一个清单 + 按模板预置任务
     *
     * @param code 模板编码
     * */
    @PostMapping("/{code}/apply")
    public R<TodoTemplateApplyVO> apply(@PathVariable String code, @RequestBody(required = false) TodoTemplateApplyDTO dto) {
        return R.ok(templateService.applyTemplate(code, dto));
    }
}
