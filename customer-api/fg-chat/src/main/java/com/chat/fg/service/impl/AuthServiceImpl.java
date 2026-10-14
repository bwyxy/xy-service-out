package com.chat.fg.service.impl;

import com.chat.fg.enums.DeviceType;
import com.chat.fg.enums.SessionStatus;
import com.chat.fg.pojo.LoginSession;
import com.chat.fg.pojo.dto.LoginDTO;
import com.chat.fg.pojo.dto.RegisterDTO;
import com.chat.fg.security.ChatUserDetails;
import com.chat.fg.service.ForceLogoutNotifier;
import com.chat.fg.service.IAuthService;
import com.chat.fg.service.ILoginSessionService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.vo.ResultVO;
import com.xy.common.client.dto.response.IMAddrResponse;
import com.xy.common.entity.User;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.EncryptionUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.framework.web.service.VerifyService;
import com.xy.system.service.wk.WKClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private VerifyService verifyService;

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private WKClientService wkClientService;

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ILoginSessionService loginSessionService;

    @Autowired
    private ForceLogoutNotifier forceLogoutNotifier;

    /**
     * 注册用户
     */
    @Override
    public Long registerUser(RegisterDTO registerDTO) {

        if (!registerDTO.isPasswordConfirmPassword()) {
            throw new SecurityException("两次密码不一致, 请确认您输入的密码");
        }

        String email = registerDTO.getEmail();
        String emailCode = registerDTO.getEmailCode();
        if (!verifyService.verifyCode(email, emailCode)) {
            throw new SecurityException("邮箱验证码不正确，请确认您的验证码");
        }

        String password = encryptionUtils.encode(registerDTO.getPassword());
        LocalDateTime now = LocalDateTime.now();
        User user = new User();

        if (registerDTO.getNickname() == null || StringUtils.isEmpty(registerDTO.getNickname())) {
            user.setNickname("用户" + registerDTO.getPhone());
        } else {
            user.setNickname(registerDTO.getNickname());
        }
        user.setPassword(password);
        user.setPhone(registerDTO.getPhone());
        user.setEmail(email);
        user.setSex(registerDTO.getSex());
        user.setCreateTime(now);
        user.setUpdateTime(now);

        try {
            userServiceImpl.save(user);

            Long uId = user.getUId();

            String content = "欢迎您使用飞鸽即时通讯。如果您在使用过程中有任何问题或建议，记得给我发消息反馈哦!";
            wkClientService.sendSystemMessage(uId, content);
            return uId;
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("uk_phone")) {
                throw new BusinessException("手机号已被注册");
            } else if (e.getMessage().contains("uk_email")) {
                throw new BusinessException("邮箱已被注册");
            }
            throw e;
        }
    }

    /**
     * 登录
     */
    @Override
    public Map<String, Object> login(LoginDTO loginDTO, String ip, String userAgent) {
        String account = loginDTO.getAccount();

        // 1. 根据手机号或邮箱查询聊天用户（t_user 表）
        User user = userServiceImpl.getByPhoneOrEmail(account, account);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }

        // 2. 校验密码
        if (!encryptionUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        Long uId = user.getUId();

        // 3. 生成 JWT
        ChatUserDetails chatUserDetails = new ChatUserDetails(user);
        String jwtToken = jwtTokenProvider.generateAccessToken(chatUserDetails);

        // 4. 多端登录管控：同设备类型旧会话全部失效并推送强制下线
        //    必须在重置 IM token 之前推送，否则旧设备可能已因 deviceLevel 抢占而断开，收不到通知
        replaceLoginSession(uId, jwtToken, loginDTO, ip, userAgent);

        // 5. 获取/更新悟空IM Token
        String uid = String.valueOf(uId);
        WKUserTokenRequest tokenRequest = new WKUserTokenRequest();
        tokenRequest.setUid(uid);
        tokenRequest.setToken(jwtToken);
        tokenRequest.setDeviceFlag(1);
        tokenRequest.setDeviceLevel(0);

        ResultVO tokenResult = wuKongClient.resetUserToken(tokenRequest);
        if (tokenResult.getStatus() != 200) {
            throw new BusinessException("获取IM连接失败: " + tokenResult.getMsg());
        }

        // 6. 获取用户IM 地址
        IMAddrResponse imAddrResponse = wuKongClient.getRoute(uId);

        // 7. 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("token", jwtToken);
        result.put("nickname", user.getNickname());
        result.put("imAddr", imAddrResponse);
        return result;
    }

    /**
     * 建立新会话并踢掉同设备类型的旧会话
     * */
    private void replaceLoginSession(Long uId, String jwtToken, LoginDTO loginDTO, String ip, String userAgent) {
        Integer submitted = loginDTO.getDeviceType();
        DeviceType deviceType = submitted != null
                ? DeviceType.fromCode(submitted)
                : DeviceType.resolveByUserAgent(userAgent);

        long now = System.currentTimeMillis();
        LoginSession session = new LoginSession();
        session.setUid(uId);
        session.setDeviceType(deviceType.getCode());
        session.setDeviceId(StringUtils.isEmpty(loginDTO.getDeviceId()) ? "unknown" : loginDTO.getDeviceId());
        session.setDeviceName(buildDeviceName(deviceType, userAgent));
        session.setToken(jwtToken);
        session.setLoginTime(now);
        session.setLastActiveTime(now);
        session.setStatus(SessionStatus.ACTIVE.getCode());
        session.setIp(ip);

        List<LoginSession> kicked = loginSessionService.replaceSession(
                session, jwtTokenProvider.getRemainingExpiration(jwtToken));
        for (LoginSession old : kicked) {
            forceLogoutNotifier.notifyKicked(old);
        }
    }

    /**
     * 生成设备描述（设备类型 + User-Agent 简写）
     * */
    private String buildDeviceName(DeviceType deviceType, String userAgent) {
        if (StringUtils.isEmpty(userAgent)) {
            return deviceType.getLabel();
        }
        String ua = userAgent.length() > 80 ? userAgent.substring(0, 80) : userAgent;
        return deviceType.getLabel() + " / " + ua;
    }

    /**
     * 登出：仅允许结束当前登录用户自身的会话
     * */
    @Override
    public void logout(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }

        LoginSession session = loginSessionService.getSession(token);
        // 越权校验：会话归属必须与当前登录用户一致
        if (session != null && !SecurityUtils.getUId().equals(session.getUid())) {
            throw new BusinessException("无权操作该登录会话");
        }

        loginSessionService.endSession(token, SessionStatus.LOGOUT);
    }
}