package com.chat.fg.controller;

import com.alibaba.fastjson.JSON;
import com.chat.fg.pojo.dto.FriendRequestDTO;
import com.chat.fg.pojo.dto.HandleFriendRequestDTO;
import com.chat.fg.security.ChatUserDetails;
import com.chat.fg.utils.DistributedLockHelper;
import com.xy.common.entity.User;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.system.service.wk.WKClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 好友控制器集成测试
 * */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FriendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private WKClientService wkClientService;

    @MockBean
    private DistributedLockHelper lockHelper;

    private String token;
    private ChatUserDetails userDetails;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUId(10001L);
        user.setNickname("用户A");
        user.setStatus("0");
        user.setPassword("$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2");
        userDetails = new ChatUserDetails(user);
        token = jwtTokenProvider.generateAccessToken(userDetails);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));

        // 分布式锁直接放行，避免集成测试依赖 Redis
        when(lockHelper.tryLock(anyString(), anyLong(), anyLong(), any(Supplier.class), anyString()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        doAnswer(invocation -> {
            invocation.getArgument(3, Runnable.class).run();
            return null;
        }).when(lockHelper).tryLock(anyString(), anyLong(), anyLong(), any(Runnable.class), anyString());
    }

    @Test
    void searchUsers_shouldReturnList() throws Exception {
        mockMvc.perform(get("/friend/search")
                        .header("token", token)
                        .param("keyword", "用户B"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("200"));
                });
    }

    @Test
    void sendAndHandleFriendRequest_fullFlow() throws Exception {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setToUid(10002L);
        dto.setApplyMsg("你好");

        // 发送好友请求
        mockMvc.perform(post("/friend/request/send")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("200"));
                });

        // 查询收到请求的用户（切换到 10002）
        User userB = new User();
        userB.setUId(10002L);
        userB.setNickname("用户B");
        userB.setStatus("0");
        userB.setPassword("$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2");
        ChatUserDetails userBDetails = new ChatUserDetails(userB);
        String tokenB = jwtTokenProvider.generateAccessToken(userBDetails);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userBDetails, null, userBDetails.getAuthorities()));

        MvcResult listResult = mockMvc.perform(get("/friend/request/received")
                        .header("token", tokenB))
                .andExpect(status().isOk())
                .andReturn();
        String listBody = listResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(listBody.contains("用户A"), "响应中应包含发送者昵称，实际响应：" + listBody);

        // 解析请求ID
        int idIndex = listBody.indexOf("\"id\":");
        int commaIndex = listBody.indexOf(",", idIndex);
        Long requestId = Long.valueOf(listBody.substring(idIndex + 5, commaIndex));

        HandleFriendRequestDTO handleDto = new HandleFriendRequestDTO();
        handleDto.setRequestId(requestId);
        handleDto.setAction(1);

        // 同意好友请求
        mockMvc.perform(post("/friend/request/handle")
                        .header("token", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(handleDto)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("200"));
                });

        // 切换回用户 A 查询好友列表
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));

        mockMvc.perform(get("/friend/list")
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    assertTrue(body.contains("用户B"), "用户A的好友列表中应包含用户B，实际响应：" + body);
                });
    }
}
