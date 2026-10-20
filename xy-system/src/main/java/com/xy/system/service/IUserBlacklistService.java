package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.chat.UserBlacklist;
import com.xy.common.vo.chat.UserBlacklistVO;

import java.util.List;

/**
 * 用户黑名单服务接口
 *
 * <p>拉黑关系为用户对用户且单向。后台管理端作为运营视角代管：可查看指定用户的黑名单、
 * 代加/代删。写入本地表的同时同步悟空 IM 频道黑名单，双重生效。</p>
 */
public interface IUserBlacklistService extends IService<UserBlacklist> {

    /**
     * 分页查询指定用户的黑名单
     *
     * @param uId      拉黑发起方 UID
     * @param targetUid 被拉黑用户 UID（可选，精确匹配）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果（附带双方昵称）
     */
    PageResult<UserBlacklistVO> selectBlacklistPage(Long uId, Long targetUid, int pageNum, int pageSize);

    /**
     * 新增拉黑关系（后台代管）
     *
     * @param uId       拉黑发起方 UID
     * @param targetUid 被拉黑用户 UID
     * @param remark    备注
     * @param operator  操作人
     */
    void addBlacklist(Long uId, Long targetUid, String remark, String operator);

    /**
     * 删除拉黑关系
     *
     * @param id 黑名单主键
     */
    void removeBlacklist(Long id);

    /**
     * 查询指定用户拉黑的全部 UID
     *
     * @param uId 拉黑发起方 UID
     * @return 被拉黑 UID 列表
     */
    List<Long> listBlockedUids(Long uId);

    /**
     * 判断 ownerUid 是否已拉黑 fromUid
     *
     * <p>供消息发送链路调用：接收方拉黑了发送方时拒绝投递。</p>
     *
     * @param ownerUid 拉黑发起方（消息接收方）
     * @param fromUid  消息发送方
     * @return true 表示被拉黑
     */
    boolean isBlocked(Long ownerUid, Long fromUid);
}
