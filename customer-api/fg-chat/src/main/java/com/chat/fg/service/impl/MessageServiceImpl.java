package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.fg.client.AiChatClient;
import com.chat.fg.mapper.ConversationSettingMapper;
import com.chat.fg.config.AiApiKeyTokenProvider;
import com.chat.fg.pojo.dto.AiChatRequest;
import com.chat.fg.pojo.dto.AiChatResponse;
import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.pojo.dto.RecallMessageDTO;
import com.chat.fg.pojo.dto.SearchMessageDTO;
import com.chat.fg.pojo.dto.SendMsgDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.pojo.vo.SearchMessageVO;
import com.chat.fg.service.IMessageService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.entity.Message;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.constant.Constants;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.ConversationSetting;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Autowired
    private ConversationSettingMapper conversationSettingMapper;

    @Value("${ai.assistant.uid:fg_ai}")
    private String aiAssistantUid;

    /** 消息撤回时间窗口（分钟） */
    private static final long RECALL_WINDOW_MINUTES = 2;

    /** 搜索聊天记录时单页回溯的条数 */
    private static final int SEARCH_PAGE_SIZE = 50;

    /** 搜索聊天记录时最多回溯的消息条数，避免全量扫描 */
    private static final int SEARCH_MAX_SCAN = 300;

    /** 按类型/日期浏览时最多回溯的消息条数（需覆盖更早的记录） */
    private static final int SEARCH_MAX_SCAN_DEEP = 1000;

    /** 搜索聊天记录返回结果条数上限 */
    private static final int SEARCH_RESULT_LIMIT = 50;

    /** 链接识别：文本中包含 http/https 地址即视为链接 */
    private static final Pattern LINK_PATTERN = Pattern.compile("https?://", Pattern.CASE_INSENSITIVE);

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
            return filterHiddenMessages(wuKongClient.messageSync(msgRequest), msgDTO);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 过滤当前用户已删除的聊天记录
     *
     * <p>删除聊天记录/删除会话只对本用户生效：记录删除时刻的最新消息序号，
     * 拉取消息时把不大于该序号的消息隐藏掉，其他用户不受影响。</p>
     * */
    private MessageVO filterHiddenMessages(MessageVO messageVO, MsgDTO msgDTO) {
        if (messageVO == null || messageVO.getMessages() == null || messageVO.getMessages().isEmpty()) {
            return messageVO;
        }

        long hiddenThroughSeq = getHiddenThroughSeq(msgDTO.getChannelId(), msgDTO.getChannelType());

        if (hiddenThroughSeq <= 0) {
            return messageVO;
        }

        List<Message> messages = messageVO.getMessages().stream()
                .filter(message -> message.getMessageSeq() == null || message.getMessageSeq() > hiddenThroughSeq)
                .collect(Collectors.toList());

        messageVO.setMessages(messages);

        return messageVO;
    }

    /**
     * 查询当前用户在该频道的删除标记（取删记录与删会话标识的较大值）
     * */
    private long getHiddenThroughSeq(String channelId, Integer channelType) {
        try {
            LambdaQueryWrapper<ConversationSetting> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(ConversationSetting::getUId, SecurityUtils.getUId())
                    .eq(ConversationSetting::getChannelId, channelId)
                    .eq(ConversationSetting::getChannelType, channelType);

            ConversationSetting setting = conversationSettingMapper.selectOne(queryWrapper);

            if (setting == null) {
                return 0L;
            }

            long clearedSeq = setting.getClearedSeq() == null ? 0L : setting.getClearedSeq();
            long deletedSeq = setting.getDeletedSeq() == null ? 0L : setting.getDeletedSeq();

            return Math.max(clearedSeq, deletedSeq);
        } catch (Exception e) {
            log.warn("查询会话删除标记失败, channelId={}, channelType={}", channelId, channelType, e);
            return 0L;
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

        return syncMessages(msgRequest);
    }

    /**
     * 同步消息，异常或空响应时返回空集合
     * */
    private List<Message> syncMessages(MsgRequest msgRequest) {
        MessageVO messageVO;

        try {
            messageVO = wuKongClient.messageSync(msgRequest);
        } catch (Exception e) {
            log.error("同步消息失败, channelId={}", msgRequest.getChannelId(), e);
            return Collections.emptyList();
        }

        if (messageVO == null || messageVO.getMessages() == null) {
            return Collections.emptyList();
        }

        return messageVO.getMessages();
    }

    /**
     * 搜索聊天记录
     *
     * <p>悟空 IM 未提供消息搜索接口，这里从最新消息开始按页向前回溯，
     * 解析 payload 后按「消息类型 / 关键字 / 链接 / 时间区间」过滤，
     * 回溯条数达到上限或无更多消息时结束。</p>
     * */
    @Override
    public List<SearchMessageVO> searchMessages(SearchMessageDTO searchMessageDTO) {
        String keyword = searchMessageDTO.getKeyword() == null ? "" : searchMessageDTO.getKeyword().trim();
        String lowerKeyword = keyword.toLowerCase();

        boolean linkOnly = Boolean.TRUE.equals(searchMessageDTO.getLinkOnly());

        Set<Integer> types = searchMessageDTO.getTypes() == null || searchMessageDTO.getTypes().isEmpty()
                ? new HashSet<>(Collections.singletonList(MessageType.text.getCode()))
                : new HashSet<>(searchMessageDTO.getTypes());

        // 关键字搜索只回溯近期消息；按类型/日期浏览需要覆盖更早的记录
        int maxScan = keyword.isEmpty() || linkOnly ? SEARCH_MAX_SCAN_DEEP : SEARCH_MAX_SCAN;

        int limit = searchMessageDTO.getLimit() != null && searchMessageDTO.getLimit() > 0
                ? Math.min(searchMessageDTO.getLimit(), SEARCH_RESULT_LIMIT)
                : SEARCH_RESULT_LIMIT;

        Long startTime = searchMessageDTO.getStartTime();
        Long endTime = searchMessageDTO.getEndTime();

        String loginUid = String.valueOf(SecurityUtils.getUId());

        List<SearchMessageVO> result = new ArrayList<>();
        int startSeq = 0;
        int scanned = 0;
        // 已回溯到时间窗口起点之前的消息，可提前结束
        boolean reachedStart = false;

        while (result.size() < limit && scanned < maxScan && !reachedStart) {
            MsgRequest msgRequest = new MsgRequest();
            msgRequest.setLoginUid(loginUid);
            msgRequest.setChannelId(searchMessageDTO.getChannelId());
            msgRequest.setChannelType(searchMessageDTO.getChannelType());
            msgRequest.setStartMessageSeq(startSeq);
            msgRequest.setEndMessageSeq(0);
            msgRequest.setLimit(SEARCH_PAGE_SIZE);
            msgRequest.setPullMode(0);

            List<Message> page = syncMessages(msgRequest);

            if (page.isEmpty()) {
                break;
            }

            for (Message message : page) {
                scanned++;

                long timestamp = toMillis(message.getTimestamp());

                // 比时间窗口更新的消息直接跳过
                if (endTime != null && timestamp > endTime) {
                    continue;
                }

                // 已早于时间窗口起点，本页处理完后即可结束
                if (startTime != null && timestamp < startTime) {
                    reachedStart = true;
                    continue;
                }

                SearchMessageVO vo = matchMessage(message, types, linkOnly, lowerKeyword);

                if (vo != null) {
                    result.add(vo);
                }
            }

            // 以本页最小序号为界继续向更早的方向回溯
            int minSeq = page.stream()
                    .map(Message::getMessageSeq)
                    .filter(Objects::nonNull)
                    .mapToInt(Long::intValue)
                    .min()
                    .orElse(0);

            if (minSeq <= 1) {
                break;
            }

            startSeq = minSeq - 1;
        }

        // 按消息序号倒序返回最近的匹配结果
        result.sort(Comparator.comparing(SearchMessageVO::getMessageSeq,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return result.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 解析消息 payload 并判断是否命中过滤条件
     *
     * @param types 允许的消息类型
     * @param linkOnly 是否仅匹配链接文本
     * @param lowerKeyword 关键字（小写），为空表示不做内容过滤
     * @return 命中时返回搜索结果，未命中返回 null
     * */
    private SearchMessageVO matchMessage(Message message, Set<Integer> types,
                                         boolean linkOnly, String lowerKeyword) {
        String payload = message.getPayload();

        if (payload == null || payload.isEmpty()) {
            return null;
        }

        JSONObject content;

        try {
            content = JSON.parseObject(new String(Base64.getDecoder().decode(payload)));
        } catch (Exception e) {
            log.warn("搜索聊天记录-解析 payload 失败, messageSeq={}", message.getMessageSeq());
            return null;
        }

        if (content == null) {
            return null;
        }

        Integer type = content.getInteger("type");
        String text = content.getString("content");

        if (type == null || !types.contains(type)) {
            return null;
        }

        boolean isText = MessageType.text.getCode().equals(type);

        if (linkOnly) {
            // 链接只在文本消息里，且需包含 http/https 地址
            if (!isText || text == null || !LINK_PATTERN.matcher(text).find()) {
                return null;
            }
        } else if (!lowerKeyword.isEmpty()) {
            // 关键字仅对文本消息生效（图片/文件存的是 URL）
            if (!isText || text == null || !text.toLowerCase().contains(lowerKeyword)) {
                return null;
            }
        }

        SearchMessageVO vo = new SearchMessageVO();
        vo.setMessageSeq(message.getMessageSeq());
        vo.setMessageId(message.getMessageId());
        vo.setFromUid(message.getFromUid());
        vo.setType(type);
        vo.setContent(text);
        vo.setTimestamp(toMillis(message.getTimestamp()));

        return vo;
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
