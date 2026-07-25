package com.xy.system.service;

import com.xy.common.entity.SysMenu;
import com.xy.common.vo.RouterVo;

import java.util.List;

public interface ISysMenuService {

    /**
     * 查询系统菜单列表
     *
     * @param menu 菜单信息
     * @return 菜单列表
     */
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId);

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<SysMenu> selectMenuTreeByUserId(Long userId);

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    public List<RouterVo> buildMenus(List<SysMenu> menus);

    /**
     * 根据菜单ID查询菜单
     */
    public SysMenu selectMenuById(Long menuId);

    /**
     * 新增菜单
     */
    public int insertMenu(SysMenu menu);

    /**
     * 修改菜单
     */
    public int updateMenu(SysMenu menu);

    /**
     * 删除菜单
     */
    public int deleteMenuById(Long menuId);

    /**
     * 校验菜单名称是否唯一
     */
    public boolean checkMenuNameUnique(SysMenu menu);
}
