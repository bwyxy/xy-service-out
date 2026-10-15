package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.DispatchRequestDTO;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.vo.blog.DispatchSubmitVO;
import com.xy.common.vo.blog.DispatchTaskVO;
import com.xy.system.service.blog.IBlogDispatchTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分发任务
 * */
@RestController
@RequestMapping("/blog/dispatch/task")
@RequiredArgsConstructor
public class BlogDispatchTaskController {

    private final IBlogDispatchTaskService taskService;

    /**
     * 分页查询分发任务
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:task:list')")
    @PostMapping("/list")
    public R<PageResult<DispatchTaskVO>> list(@RequestBody BlogDispatchTask query) {
        return R.ok(taskService.selectTaskPage(query));
    }

    /**
     * 查询任务详情
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:task:list')")
    @GetMapping("/{id}")
    public R<DispatchTaskVO> detail(@PathVariable Long id) {
        return R.ok(taskService.selectTaskById(id));
    }

    /**
     * 提交分发（单篇/多篇、单平台/多平台）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:task:dispatch')")
    @PostMapping("/dispatch")
    public R<DispatchSubmitVO> dispatch(@RequestBody DispatchRequestDTO request) {
        return R.ok(taskService.submitDispatch(request));
    }

    /**
     * 重试任务（单个或批量）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:task:retry')")
    @PostMapping("/retry")
    public R<String> retry(@RequestBody List<Long> taskIds) {
        taskService.retryTasks(taskIds);
        return R.ok("已提交重试");
    }

    /**
     * 删除任务（单个或批量）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:task:remove')")
    @PostMapping("/remove")
    public R<String> remove(@RequestBody List<Long> taskIds) {
        taskService.deleteTasks(taskIds);
        return R.ok("删除成功");
    }
}
