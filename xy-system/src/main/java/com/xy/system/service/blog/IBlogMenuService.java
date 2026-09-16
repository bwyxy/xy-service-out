package com.xy.system.service.blog;

import com.xy.common.dto.blog.AddMenuDTO;
import com.xy.common.dto.blog.EditMenuDTO;
import com.xy.common.entity.blog.BlogMenu;

import java.util.List;

public interface IBlogMenuService {

    /**
     * 获取菜单列表(tree结构)
     * */
    List<BlogMenu> selectMenuTree();

    /**
     * 根据Id查询菜单信息
     * */
    BlogMenu selectMenuById(Long id);

    /**
     * 添加菜单
     * */
    void addMenu(AddMenuDTO addMenuDTO);

    /**
     * 编辑菜单
     * */
    void updateMenu(EditMenuDTO editMenuDTO);

    /**
     * 删除菜单
     * */
    void deleteMenu(Long id);
}
