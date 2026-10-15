package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.DispatchStatQueryDTO;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.vo.blog.DispatchStatSummaryVO;

import java.util.List;

/**
 * 分发运营数据服务
 * */
public interface IBlogDispatchStatService {

    /**
     * 分页查询分发运营数据
     * */
    PageResult<BlogDispatchArticleStat> selectStatPage(DispatchStatQueryDTO query);

    /**
     * 按平台聚合统计运营数据
     * */
    List<DispatchStatSummaryVO> selectPlatformSummary(String platformCode);

    /**
     * 分发成功时登记运营数据记录
     * */
    void recordPublishSuccess(Long taskId, Long articleId, String platformCode,
                              String platformArticleId, String resultUrl);

    /**
     * 拉取各平台运营数据并入库，返回成功采集的文章数
     * */
    int refreshStats();
}
