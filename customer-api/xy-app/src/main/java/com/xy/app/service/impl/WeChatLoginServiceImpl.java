package com.xy.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.app.pojo.vo.WeChatLoginVo;
import com.xy.app.response.WechatSessionResponse;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.ITagService;
import com.xy.app.service.IWeChatLoginService;
import com.xy.common.entity.User;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.system.service.IChatUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 微信小程序登录
 *
 * <p>用户体系为客户端用户表 t_user（主键 u_id），不写后台账号表 sys_user；
 * 登录态主题为 {@code todo_{uId}}，由 TodoUserDetailsService 解析。</p>
 * */
@Service
@RequiredArgsConstructor
public class WeChatLoginServiceImpl implements IWeChatLoginService {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    private final IChatUserService chatUserService;

    private final ITagService tagService;

    private final JwtTokenProvider jwtTokenProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeChatLoginVo login(String code) {

        WechatSessionResponse session = getSessionByCode(code);
        String openid = session.getOpenid();

        User user = chatUserService.getByOpenId(openid);
        if (user == null) {
            user = chatUserService.registerByOpenId(openid);
        }

        // 首次登录给他自己的默认清单（幂等，已有默认清单则跳过）
        tagService.initDefaultTags(user.getUId());

        String token = jwtTokenProvider.createWeChatToken(TodoUserDetails.toUsername(user.getUId()));

        WeChatLoginVo weChatLoginVo = new WeChatLoginVo();
        weChatLoginVo.setUserId(user.getUId());
        weChatLoginVo.setOpenid(openid);
        weChatLoginVo.setToken(token);
        weChatLoginVo.setNickname(user.getNickname());
        weChatLoginVo.setAvatar(user.getAvatar());

        return weChatLoginVo;
    }

    /**
     * 通过前端小程序的 code 换取 openId 和 session_key
     * */
    public WechatSessionResponse getSessionByCode(String code) {

        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code
        );

        try {
            String response = restTemplate.getForObject(url, String.class);

            WechatSessionResponse wechatSessionResponse = objectMapper.readValue(response, WechatSessionResponse.class);

            if (wechatSessionResponse.getErrcode() != null && wechatSessionResponse.getErrcode() != 0) {
                throw new RuntimeException(wechatSessionResponse.getErrmsg());
            }

            return wechatSessionResponse;
        } catch (Exception e) {
            throw new RuntimeException("调用微信接口失败", e);
        }
    }
}
