package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogNotice;
import com.xy.common.vo.blog.NoticeVO;

import java.util.List;

public interface IBlogNoticeService {

    /**
     * 分页查询公告列表
     * */
    PageResult<BlogNotice> selectBlogNotice(BlogNotice blogNotice);

    /**
     * 查询公告列表
     * */
    List<NoticeVO> selectNoticeList();

    /**
     * 查询公告详情
     * */
    BlogNotice selectBlogNoticeById(Long id);

    /**
     * 添加公告
     * */
    void addBlogNotice(BlogNotice blogNotice);

    /**
     * 更新公告
     * */
    void updateBlogNotice(BlogNotice blogNotice);

    /**
     * 删除公告
     * */
    void deleteBlogNotice(Long id);
}
