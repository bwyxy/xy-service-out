package com.xy.system.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.dto.blog.DispatchStatQueryDTO;
import com.xy.common.vo.blog.DispatchStatSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogDispatchArticleStatMapper extends BaseMapper<BlogDispatchArticleStat> {

    /**
     * 分页查询分发运营数据（联表补充文章标题）
     * */
    IPage<BlogDispatchArticleStat> selectStatPage(IPage<BlogDispatchArticleStat> page,
                                                  @Param("query") DispatchStatQueryDTO query);

    /**
     * 按平台聚合统计运营数据
     * */
    List<DispatchStatSummaryVO> selectPlatformSummary(@Param("platformCode") String platformCode);
}
