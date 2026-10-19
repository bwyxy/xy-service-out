package com.chat.fg.service;

import com.chat.fg.pojo.dto.ConversationChannelDTO;
import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.dto.ConversationTopDTO;
import com.chat.fg.pojo.vo.UserConversationVO;

import java.util.List;

/**
 * 会话 逻辑层
 * */
public interface IConversationService {

    /**
     * 获取会话列表
     * */
    public List<UserConversationVO> getConversations(ConversationDTO conversationDTO);

    /**
     * 设置/取消会话置顶
     * */
    public void updateTop(ConversationTopDTO conversationTopDTO);

    /**
     * 删除聊天记录
     *
     * <p>仅对当前用户生效：记录删除时刻的最新消息序号，之后不再展示该序号及更早的消息，
     * 会话本身保留，对方仍可正常查看。</p>
     * */
    public void clearMessages(ConversationChannelDTO conversationChannelDTO);

    /**
     * 删除会话
     *
     * <p>仅对当前用户生效：会话从列表隐藏并清空聊天记录，
     * 收到新消息后会话会重新出现在列表中，对方不受影响。</p>
     * */
    public void deleteConversation(ConversationChannelDTO conversationChannelDTO);
}
