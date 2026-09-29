package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.blog.pojo.dto.PhotoDTO;
import com.xy.blog.pojo.vo.PhotoVO;
import com.xy.blog.service.IPhotoService;
import com.xy.common.entity.blog.BlogPhoto;
import com.xy.system.service.impl.blog.BlogPhotoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PhotoServiceImpl implements IPhotoService {

    @Autowired
    private BlogPhotoServiceImpl blogPhotoService;

    @Override
    public List<PhotoVO> selectPhotoList(PhotoDTO photoDTO) {
        LambdaQueryWrapper<BlogPhoto> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(BlogPhoto::getCategoryId, photoDTO.getCategoryId())
                .eq(BlogPhoto::getStatus, 1)
                .orderByDesc(BlogPhoto::getPublishedTime)
                .orderByDesc(BlogPhoto::getSort);

        Page<BlogPhoto> page = blogPhotoService.page(new Page<>(photoDTO.getPageNum(), photoDTO.getPageSize()), wrapper);
        List<BlogPhoto> list = page.getRecords();
        List<PhotoVO> photoVOList = new ArrayList<>();

        for (BlogPhoto blogPhoto : list) {
            PhotoVO photoVO = new PhotoVO();

            photoVO.setId(blogPhoto.getId());
            photoVO.setName(blogPhoto.getName());
            photoVO.setUrl(blogPhoto.getPhotoUrl());
            photoVO.setDescription(blogPhoto.getDescription());

            photoVOList.add(photoVO);
        }

        return photoVOList;
    }
}
