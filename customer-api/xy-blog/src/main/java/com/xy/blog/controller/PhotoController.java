package com.xy.blog.controller;

import com.xy.blog.pojo.dto.PhotoDTO;
import com.xy.blog.pojo.vo.PhotoVO;
import com.xy.blog.service.IPhotoService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 相册
 * */
@RestController
@RequestMapping("/photo")
public class PhotoController {

    @Autowired
    private IPhotoService photoService;

    /**
     * 查询相册列表 (公开)
     * */
    @Anonymous
    @PostMapping("/list")
    public R<List<PhotoVO>> selectPhotoList(@RequestBody PhotoDTO photoDTO) {
        List<PhotoVO> photoList = photoService.selectPhotoList(photoDTO);

        return R.ok(photoList);
    }
}
