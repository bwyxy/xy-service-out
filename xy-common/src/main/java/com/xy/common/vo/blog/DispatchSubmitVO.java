package com.xy.common.vo.blog;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分发任务提交结果 VO
 * */
@Data
public class DispatchSubmitVO {

    /** 批次号 */
    private String batchNo;

    /** 成功创建的任务数 */
    private int createdCount;

    /** 被跳过的数量（已分发过或平台不可用） */
    private int skippedCount;

    /** 创建出来的任务 Id 列表 */
    private List<Long> taskIds = new ArrayList<>();

    /** 跳过原因明细 */
    private List<String> skippedReasons = new ArrayList<>();
}
