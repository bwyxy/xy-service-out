package com.xy.blog.service;

import com.xy.blog.pojo.dto.PhotoDTO;
import com.xy.blog.pojo.vo.PhotoVO;

import java.util.List;

public interface IPhotoService {

    List<PhotoVO> selectPhotoList(PhotoDTO photoDTO);
}
