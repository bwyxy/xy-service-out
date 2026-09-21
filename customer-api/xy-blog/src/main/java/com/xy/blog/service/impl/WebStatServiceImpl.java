package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.blog.pojo.vo.WebStatVO;
import com.xy.blog.service.IWebStatService;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.SysCategory;
import com.xy.common.entity.blog.BlogEventLog;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.system.mapper.blog.BlogEventLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class WebStatServiceImpl implements IWebStatService {

    @Autowired
    private ArticleServiceImpl articleService;

    @Autowired
    private CategoryServiceImpl categoryService;

    @Autowired
    private BlogEventLogMapper blogEventLogMapper;

    public WebStatVO getWebStat() {
        WebStatVO webStatVO = new WebStatVO();

        webStatVO.setArticleCount(getArticleCount());
        webStatVO.setCategoryCount(getCategoryCount());
        webStatVO.setPV(getPV());

        return webStatVO;
    }

    private long getArticleCount() {
        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysArticle::getStatus, 1);

        return articleService.count(queryWrapper);
    }

    private long getCategoryCount() {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();

        return categoryService.count(queryWrapper);
    }

    private long getPV() {
        LocalDateTime now = LocalDateTime.now();

        VisitDailyStatsVO totalStats = blogEventLogMapper.selectTotalPvUv(now);

        return totalStats.getPv();
    }
}
