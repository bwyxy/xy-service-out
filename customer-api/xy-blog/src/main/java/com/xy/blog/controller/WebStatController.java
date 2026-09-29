package com.xy.blog.controller;

import com.xy.blog.pojo.vo.WebStatVO;
import com.xy.blog.service.IWebStatService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点统计
 * */
@RequestMapping("/web")
@RestController
public class WebStatController {

    @Autowired
    private IWebStatService webStatService;

    /**
     * 统计访问量
     * */
    @Anonymous
    @GetMapping("/stat")
    public R<WebStatVO> getWebStat(){
        WebStatVO webStatVO = webStatService.getWebStat();

        return R.ok(webStatVO);
    }
}
