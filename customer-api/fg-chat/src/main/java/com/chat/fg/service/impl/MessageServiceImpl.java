package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chat.fg.client.AiChatClient;
import com.chat.fg.config.AiApiKeyTokenProvider;
import com.chat.fg.pojo.dto.AiChatRequest;
import com.chat.fg.pojo.dto.AiChatResponse;
import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.pojo.dto.RecallMessageDTO;
import com.chat.fg.pojo.dto.SendMsgDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.service.IMessageService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.entity.Message;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.constant.Constants;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MessageServiceImpl implements IMessageService {

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private AiChatClient aiChatClient;

    @Autowired
    private AiApiKeyTokenProvider aiApiKeyTokenProvider;

    @Autowired
    private GroupServiceImpl groupService;

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    @Value("${ai.assistant.uid:fg_ai}")
    private String aiAssistantUid;

    /** 消息撤回时间窗口（分钟） */
    private static final long RECALL_WINDOW_MINUTES = 2;

    /**
     * 查询频道历史消息
     * */
    public MessageVO getMessageSync(MsgDTO msgDTO) {
        Long uId = SecurityUtils.getUId();

        MsgRequest msgRequest = new MsgRequest();

        if (msgDTO.getLimit() == 0) {
            msgRequest.setLimit(30);
        } else {
            msgRequest.setLimit(msgDTO.getLimit());
        }

        msgRequest.setLoginUid(String.valueOf(uId));
        msgRequest.setChannelId(msgDTO.getChannelId());
        msgRequest.setChannelType(msgDTO.getChannelType());
        msgRequest.setPullMode(msgDTO.getPullMode());
        msgRequest.setStartMessageSeq(msgDTO.getStartMessageSeq());
        msgRequest.setEndMessageSeq(msgDTO.getEndMessageSeq());

        log.warn("校验参数-{}", msgRequest);
        try {
            return wuKongClient.messageSync(msgRequest);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 发送消息（经服务端转发至悟空 IM）
     *
     * <p>统一由服务端构造 payload 并调用悟空 IM /message/send 接口，
     * 便于后续做内容安全校验、敏感词过滤、图片鉴黄等业务扩展。</p>
     * */
    public Object sendMessage(SendMsgDTO sendMsgDTO) {
        Long uId = SecurityUtils.getUId();

        // 群聊消息：校验全员禁言与成员禁言
        if (ChannelType.GROUP_CHANNEL.getCode().equals(sendMsgDTO.getChannelType())) {
            checkGroupSpeakPermission(uId, sendMsgDTO.getChannelId());
        }

        // 1. 构造 payload：{ type, content, quote?, mentions? }
        JSONObject payload = new JSONObject();
        payload.put("type", sendMsgDTO.getType());
        payload.put("content", sendMsgDTO.getContent());
        if (sendMsgDTO.getQuote() != null) {
            payload.put("quote", sendMsgDTO.getQuote());
        }
        if (sendMsgDTO.getMentions() != null && !sendMsgDTO.getMentions().isEmpty()) {
            payload.put("mentions", sendMsgDTO.getMentions());
        }
        String payloadBase64 = Base64.getEncoder().encodeToString(payload.toJSONString().getBytes());

        // 2. 构造悟空 IM 发送请求
        MessageSendRequest request = new MessageSendRequest();
        request.setFromUid(String.valueOf(uId));
        request.setChannelId(sendMsgDTO.getChannelId());
        request.setChannelType(sendMsgDTO.getChannelType());
        request.setPayload(payloadBase64);
        request.setClientMsgNo(sendMsgDTO.getClientMsgNo());
        if (request.getClientMsgNo() == null || request.getClientMsgNo().isEmpty()) {
            request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));
        }

        // 默认持久化、显示红点
        MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
        // RTC 实时信令（offer/answer/candidate 等 200-208）：不持久化、不显示红点
        boolean isRtcSignal = sendMsgDTO.getType() != null
                && sendMsgDTO.getType() >= 200 && sendMsgDTO.getType() <= 208;
        header.setNoPersist(isRtcSignal ? 1 : 0);
        header.setRedDot(isRtcSignal ? 0 : 1);
        request.setHeader(header);

        log.info("发送消息到悟空 IM, fromUid={}, channelId={}, type={}",
                uId, sendMsgDTO.getChannelId(), sendMsgDTO.getType());

        try {
            Object result = wuKongClient.sendMessage(request);

            // 若用户发送给 AI 助手，异步调用 AI 并回复
            if (aiAssistantUid.equals(sendMsgDTO.getChannelId())) {
                String token = aiApiKeyTokenProvider.getToken();
                log.info("AI 助手请求, userId={}, tokenExists={}", uId, token != null && !token.isEmpty());
                CompletableFuture.runAsync(() -> sendAiReply(uId, sendMsgDTO, token));
            }

            return result;
        } catch (Exception e) {
            log.error("发送消息到悟空 IM 失败", e);
            throw new BusinessException("发送消息失败：" + e.getMessage());
        }
    }

    /**
     * 群聊发言权限校验
     *
     * <p>1. 群已解散时禁止发送；2. 成员处于禁言期时禁止发送；
     * 3. 群主开启全员禁言时仅群主/管理员可发送。</p>
     * */
    private void checkGroupSpeakPermission(Long uId, String channelId) {
        if (channelId == null || !channelId.startsWith(Constants.GROUP_ID_PREFIX)) {
            return;
        }

        Long groupId = Long.valueOf(channelId.substring(Constants.GROUP_ID_PREFIX.length()));

        Group group = groupService.getById(groupId);

        if (group == null || Integer.valueOf(1).equals(group.getDisband())) {
            throw new BusinessException("群聊不存在或已解散");
        }

        GroupMember member = groupMemberService.getGroupMemberByUid(uId, groupId);

        if (member != null && member.getMuteEndTime() != null
                && member.getMuteEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("您已被禁言，暂时无法发送消息");
        }

        boolean isAdmin = member != null && member.getRole() != GroupMemberRole.regular.getCode();

        if (Integer.valueOf(1).equals(group.getIsBan()) && !isAdmin) {
            throw new BusinessException("群主已开启全员禁言，仅群主/管理员可发言");
        }
    }

    /**
     * 调用 AI 服务并作为 AI 助手发送回复
     */
    private void sendAiReply(Long userId, SendMsgDTO sendMsgDTO, String token) {
        try {
            List<Long> knowledgeBaseIds = new ArrayList<>();
            knowledgeBaseIds.add(1L);

            AiChatRequest aiRequest = new AiChatRequest();
            aiRequest.setUserId(userId);
            aiRequest.setConversationId(userId);
            aiRequest.setUserType("CLIENT");
            aiRequest.setKnowledgeBaseIds(knowledgeBaseIds);
            aiRequest.setModel("deepseek-v4-pro");
            aiRequest.setMessage(sendMsgDTO.getContent());
            aiRequest.setEnableTools(false);

            R<AiChatResponse> aiResponse = aiChatClient.chat(token, aiRequest);
            if (aiResponse == null || aiResponse.getData() == null) {
                log.warn("AI 服务返回为空");
                return;
            }

            AiChatResponse data = aiResponse.getData();
            if (data.getContent() == null || data.getContent().isEmpty()) {
                log.warn("AI 回复内容为空");
                return;
            }

            // 构造 AI 回复消息 payload
            JSONObject payload = new JSONObject();
            payload.put("type", sendMsgDTO.getType());
            payload.put("content", data.getContent());
            String payloadBase64 = Base64.getEncoder().encodeToString(payload.toJSONString().getBytes());

            MessageSendRequest replyRequest = new MessageSendRequest();
            replyRequest.setFromUid(aiAssistantUid);
            replyRequest.setChannelId(String.valueOf(userId));
            replyRequest.setChannelType(1);
            replyRequest.setPayload(payloadBase64);
            replyRequest.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

            MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
            header.setNoPersist(0);
            header.setRedDot(1);
            replyRequest.setHeader(header);

            wuKongClient.sendMessage(replyRequest);
            log.info("AI 助手回复已发送, userId={}, contentLength={}", userId, data.getContent().length());
        } catch (Exception e) {
            log.error("AI 助手回复失败", e);
        }
    }

    /**
     * 撤回消息
     *
     * <p>校验：消息是否存在、撤回人是否为原消息发送者（群聊中群主/管理员可撤回他人消息）、
     * 是否处于撤回窗口期内。校验通过后向频道广播 type=102 的撤回信令，
     * 信令持久化，保证换设备/重进会话后仍能同步撤回态。</p>
     * */
    @Override
    public void recallMessage(RecallMessageDTO recallMessageDTO) {
        Long uId = SecurityUtils.getUId();

        Message origin = findMessageBySeq(recallMessageDTO);

        if (origin == null) {
            throw new BusinessException("消息不存在或已被撤回");
        }

        if (!String.valueOf(uId).equals(origin.getFromUid()) && !canRecallOthers(uId, recallMessageDTO)) {
            throw new BusinessException("只能撤回自己发送的消息");
        }

        long elapsedMinutes = (System.currentTimeMillis() - toMillis(origin.getTimestamp())) / 60000;

        if (elapsedMinutes > RECALL_WINDOW_MINUTES) {
            throw new BusinessException("超过 " + RECALL_WINDOW_MINUTES + " 分钟的消息不能撤回");
        }

        JSONObject content = new JSONObject();
        content.put("originalMessageSeq", origin.getMessageSeq());
        content.put("originalClientMsgNo", origin.getClientMsgNo());
        content.put("fromUid", origin.getFromUid());

        JSONObject payload = new JSONObject();
        payload.put("type", MessageType.recall.getCode());
        payload.put("content", content);

        String payloadBase64 = Base64.getEncoder().encodeToString(payload.toJSONString().getBytes());

        MessageSendRequest request = new MessageSendRequest();
        request.setFromUid(String.valueOf(uId));
        request.setChannelId(recallMessageDTO.getChannelId());
        request.setChannelType(recallMessageDTO.getChannelType());
        request.setPayload(payloadBase64);
        request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

        MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
        // 持久化以便历史消息同步撤回态，但不占用未读红点
        header.setNoPersist(0);
        header.setRedDot(0);
        request.setHeader(header);

        try {
            wuKongClient.sendMessage(request);
            log.info("撤回信令已广播, uId={}, channelId={}, messageSeq={}",
                    uId, recallMessageDTO.getChannelId(), recallMessageDTO.getMessageSeq());
        } catch (Exception e) {
            log.error("广播撤回信令失败", e);
            throw new BusinessException("撤回消息失败：" + e.getMessage());
        }
    }

    /**
     * 按消息序号查询原消息
     *
     * <p>先用 [seq-1, seq+1] 的窄窗口精确查询；悟空 IM messageSync 对
     * start/end 边界是否包含在不同版本存在差异，窄窗口取不到时回退为
     * 拉取频道近期消息再按序号匹配（撤回窗口仅 2 分钟，目标必然在近期消息内）。</p>
     * */
    private Message findMessageBySeq(RecallMessageDTO recallMessageDTO) {
        Long messageSeq = recallMessageDTO.getMessageSeq();

        Message origin = syncChannelMessages(
                recallMessageDTO, (int) Math.max(messageSeq - 1, 0), (int) (messageSeq + 1), 2)
                .stream()
                .filter(msg -> messageSeq.equals(msg.getMessageSeq()))
                .findFirst()
                .orElse(null);

        if (origin != null) {
            return origin;
        }

        List<Message> recentMessages = syncChannelMessages(recallMessageDTO, 0, 0, 100);

        origin = recentMessages.stream()
                .filter(msg -> messageSeq.equals(msg.getMessageSeq()))
                .findFirst()
                .orElse(null);

        if (origin == null) {
            log.warn("撤回-未找到原消息, channelId={}, channelType={}, messageSeq={}, 近期消息序号={}",
                    recallMessageDTO.getChannelId(), recallMessageDTO.getChannelType(), messageSeq,
                    recentMessages.stream().map(Message::getMessageSeq).toList());
        }

        return origin;
    }

    /**
     * 同步频道消息
     * */
    private List<Message> syncChannelMessages(RecallMessageDTO recallMessageDTO,
                                             int startSeq, int endSeq, int limit) {
        MsgRequest msgRequest = new MsgRequest();
        msgRequest.setLoginUid(String.valueOf(SecurityUtils.getUId()));
        msgRequest.setChannelId(recallMessageDTO.getChannelId());
        msgRequest.setChannelType(recallMessageDTO.getChannelType());
        msgRequest.setStartMessageSeq(startSeq);
        msgRequest.setEndMessageSeq(endSeq);
        msgRequest.setLimit(limit);
        msgRequest.setPullMode(0);

        MessageVO messageVO = wuKongClient.messageSync(msgRequest);

        if (messageVO == null || messageVO.getMessages() == null) {
            return Collections.emptyList();
        }

        return messageVO.getMessages();
    }

    /**
     * 群聊中群主/管理员可撤回他人消息
     * */
    private boolean canRecallOthers(Long uId, RecallMessageDTO recallMessageDTO) {
        if (!ChannelType.GROUP_CHANNEL.getCode().equals(recallMessageDTO.getChannelType())) {
            return false;
        }
        if (recallMessageDTO.getChannelId() == null
                || !recallMessageDTO.getChannelId().startsWith(Constants.GROUP_ID_PREFIX)) {
            return false;
        }

        Long groupId = Long.valueOf(recallMessageDTO.getChannelId().substring(Constants.GROUP_ID_PREFIX.length()));
        GroupMember member = groupMemberService.getGroupMemberByUid(uId, groupId);

        return member != null && member.getRole() != GroupMemberRole.regular.getCode();
    }

    /**
     * 悟空回传的消息时间为秒级时间戳，统一转换为毫秒
     * */
    private long toMillis(Integer timestamp) {
        if (timestamp == null) {
            return System.currentTimeMillis();
        }

        return timestamp * 1000L;
    }

}
