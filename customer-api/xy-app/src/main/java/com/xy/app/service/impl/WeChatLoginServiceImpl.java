package com.xy.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.app.pojo.vo.WeChatLoginVo;
import com.xy.app.response.WechatSessionResponse;
import com.xy.app.service.IWeChatLoginService;
import com.xy.common.entity.SysUser;
import com.xy.common.utils.StringUtils;
import com.xy.framework.provider.JwtTokenProvider;
import com.xy.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WeChatLoginServiceImpl implements IWeChatLoginService {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Autowired
    private ISysUserService sysUserService;

    private final JwtTokenProvider jwtTokenProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeChatLoginVo login(String code) {

        WechatSessionResponse session = getSessionByCode(code);
        String openid = session.getOpenid();

        SysUser sysUser = sysUserService.getUserByOpenId(openid);
        if (sysUser == null) {
            sysUser = addWeChatUser(openid);
        }

        String token = jwtTokenProvider.createWeChatToken(sysUser.getUsername());

        WeChatLoginVo weChatLoginVo = new WeChatLoginVo();
        weChatLoginVo.setUserId(sysUser.getId());
        weChatLoginVo.setOpenid(openid);
        weChatLoginVo.setToken(token);
        weChatLoginVo.setNickname(sysUser.getNickname());
        weChatLoginVo.setAvatar(sysUser.getAvatar());

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

    /**
     * 添加微信小程序用户
     * */
    public SysUser addWeChatUser(String openid) {
        SysUser sysUser = new SysUser();

        sysUser.setUsername(openid);
        sysUser.setNickname("微信用户" + StringUtils.maskOpenId(openid));
        sysUser.setWxOpenId(openid);
        sysUser.setCreateBy("weChatRegister");

        sysUserService.addUser(sysUser);

        return sysUser;
    }
}
