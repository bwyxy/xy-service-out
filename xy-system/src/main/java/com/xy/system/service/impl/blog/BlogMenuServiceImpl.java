package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.dto.blog.AddMenuDTO;
import com.xy.common.dto.blog.EditMenuDTO;
import com.xy.common.entity.blog.BlogMenu;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.mapper.blog.BlogMenuMapper;
import com.xy.system.service.blog.IBlogMenuService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class BlogMenuServiceImpl extends ServiceImpl<BlogMenuMapper, BlogMenu> implements IBlogMenuService {

    public static final Long MENU_ROOT_ID = 0L;

    /**
     * 获取菜单列表(tree结构)
     * */
    @Override
    public List<BlogMenu> selectMenuTree() {
        try {
            LambdaQueryWrapper<BlogMenu> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(BlogMenu::getStatus, 1)
                    .eq(BlogMenu::getDeleted, 0)
                    .orderByAsc(BlogMenu::getOrderNum);

            List<BlogMenu> menus = this.getBaseMapper().selectList(queryWrapper);

            return getChildPerms(menus, MENU_ROOT_ID);
        } catch (Exception e) {
            log.error("异常处理", e);
            throw new ServiceException(e.getMessage());
        }
    }

    private List<BlogMenu> getChildPerms(List<BlogMenu> list, long parentId) {

        List<BlogMenu> returnList = new ArrayList<>();

        for (BlogMenu t : list) {
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }

        return returnList;
    }

    /**
     * 递归列表
     *
     * @param list 分类表
     * @param t 子节点
     */
    private void recursionFn(List<BlogMenu> list, BlogMenu t)
    {
        // 得到子节点列表
        List<BlogMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (BlogMenu tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<BlogMenu> getChildList(List<BlogMenu> list, BlogMenu t)
    {
        List<BlogMenu> tlist = new ArrayList<>();
        for (BlogMenu n : list) {
            if (n.getParentId().longValue() == t.getId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<BlogMenu> list, BlogMenu t)
    {
        return getChildList(list, t).size() > 0;
    }

    /**
     * 根据Id查询菜单信息
     * */
    @Override
    public BlogMenu selectMenuById(Long id) {
        if (id == null) {
            throw new ServiceException("id 不能为空");
        }

        LambdaQueryWrapper<BlogMenu> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(BlogMenu::getId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 添加菜单
     * */
    @Override
    public void addMenu(AddMenuDTO addMenuDTO) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        BlogMenu blogMenu = new BlogMenu();

        blogMenu.setName(addMenuDTO.getName());
        blogMenu.setParentId(addMenuDTO.getParentId());
        blogMenu.setOrderNum(addMenuDTO.getOrderNum());
        blogMenu.setPath(addMenuDTO.getPath());
        blogMenu.setRouteName(addMenuDTO.getRouteName());
        blogMenu.setIsFrame(addMenuDTO.getIsFrame());
        blogMenu.setStatus(addMenuDTO.getStatus());
        blogMenu.setIcon(addMenuDTO.getIcon());
        blogMenu.setCreateTime(now);
        blogMenu.setUpdateTime(now);

        save(blogMenu);
    }

    /**
     * 编辑菜单
     * */
    @Override
    public void updateMenu(EditMenuDTO editMenuDTO) {
        LambdaUpdateWrapper<BlogMenu> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(BlogMenu::getId, editMenuDTO.getId())
                .set(BlogMenu::getName, editMenuDTO.getName())
                .set(BlogMenu::getParentId, editMenuDTO.getParentId())
                .set(BlogMenu::getOrderNum, editMenuDTO.getOrderNum())
                .set(BlogMenu::getPath, editMenuDTO.getPath())
                .set(BlogMenu::getRouteName, editMenuDTO.getRouteName())
                .set(BlogMenu::getIsFrame, editMenuDTO.getIsFrame())
                .set(BlogMenu::getStatus, editMenuDTO.getStatus())
                .set(BlogMenu::getIcon, editMenuDTO.getIcon())
                .set(BlogMenu::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 删除菜单
     * */
    @Override
    public void deleteMenu(Long id) {
        LambdaUpdateWrapper<BlogMenu> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(BlogMenu::getId, id)
                .set(BlogMenu::getDeleted, 1)
                .set(BlogMenu::getUpdateTime, now);

        update(updateWrapper);
    }
}
