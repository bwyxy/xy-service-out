# 踩坑实录：实现"微信式"多端登录管控，我修了 3 个真实缺陷

> 需求：像微信一样——PC、平板、手机三类设备各自分组，**同类型设备只允许一个在线**，新登录把同类旧设备踢下线。
> 看起来是个"加个 Redis 就完事"的需求，实际上挖出了三个真缺陷，其中两个是**安全级**的。

## 0. 先定义清楚"多端"到底是什么意思

不要一上来就写代码。先明确**维度**：

- **按设备类型分组**：PC 端 / 平板端 / 手机端，三组**互相独立**，可以同时在线；
- **组内互斥**：同一组内**只允许一个活跃会话**，新登录踢掉旧的。

也就是**"三端共存，同端互踢"**。这个语义直接决定了存储结构：

```
login:session:{token}                    → 会话详情（JSON）
login:user:{uid}                         → 该用户全部活跃 token 集合
login:user:{uid}:type:{deviceType}       → 该用户该类型下的活跃 token
login:lock:{uid}:{deviceType}            → 分布式锁（串行化同端登录）
```

`key` 设计成**可读**的，排查问题时 `redis-cli keys 'login:*'` 一眼就能看懂，比纯 hash 强太多。会话用 `StringRedisTemplate` 存 JSON 字符串，**TTL 与 token 剩余有效期对齐**——token 过期，会话自动消失，不用额外清理。

---

## 坑 1（安全级）：JWT 没有 `jti`，同一秒登录生成完全相同的 token

**现象**：用户在同一秒内点了两次登录（比如手抖双击），结果"踢下线"逻辑**把刚登录的新会话也踢了**。

**定位**：我们的踢人逻辑是**按 token 精确踢除**——旧 token 拉黑、推送下线通知。但排查时发现两次登录拿到的 token **一模一样**。

**根因**：JWT 的 payload 是 `sub + iat + exp` 的组合。`iat`（签发时间）**只精确到秒**，所以**同一用户、同一秒内的两次签发，payload 完全相同**，签名自然也一样 → token 字符串雷同。

于是"按 token 区分新旧会话"这个前提直接失效：新 token == 旧 token，踢旧的就是踢新的。

**解法**：给每个 token 加一个唯一 ID（JWT 标准字段 `jti`）：

```java
public String createToken(...) {
    return Jwts.builder()
        // ... 其它 claim
        .setId(UUID.randomUUID().toString().replace("-", ""))   // jti：保证 token 全局唯一
        .signWith(...)
        .compact();
}
```

并可提供 `getJtiFromToken(token)` 供后续按 jti 维度做更细粒度的会话管理。

**教训**：
> 凡是"**按 token 区分会话**"的逻辑，都必须保证 **token 本身唯一**。
> JWT 默认**不保证**这一点——`iat` 秒级精度 + 相同 claim = 相同 token。
> **加 `jti` 是一行代码的事，不加就是一个安全漏洞。**

---

## 坑 2（安全级）：`finally` 里放 `doFilter`，让整个 Token 黑名单形同虚设

这是本次最惊悚的一个。**Token 黑名单校验看起来完全正常，实际上完全没有生效。**

**现象**：被踢下线的旧 token，理论上应该立刻失效。但实测发现——**它居然还能继续访问接口。**

**定位**：看 [JwtAuthenticationFilter.java](file:///Users/xiaoyi/me-project/xy-service-out/xy-framework/src/main/java/com/xy/framework/security/filter/JwtAuthenticationFilter.java) 的原始写法：

```java
// ❌ 错误写法
try {
    String token = getToken(request);
    if (blacklistService.isBlacklisted(token)) {
        // 这里想拦截，但……
        throw new RuntimeException("token 已失效");
    }
    // 认证逻辑……
} catch (Exception e) {
    log.warn("认证异常", e);
} finally {
    filterChain.doFilter(request, response);   // ⚠️ 真正的凶手
}
```

**根因**：`finally` **一定会执行**。哪怕中间 `throw` 了异常、哪怕黑名单命中了，`finally` 里的 `filterChain.doFilter(...)` 仍然会把请求**继续往下传给控制器**。

所以流程变成了："检测到黑名单 → 抛异常 → 被 catch 吞掉 → finally 继续放行 → 请求正常执行"。

**黑名单校验 = 装饰品。**

**解法**：**黑名单命中必须显式 `return`，不能靠抛异常 + finally 兜底。**

```java
// ✅ 正确写法
try {
    String token = getToken(request);
    if (blacklistService.isBlacklisted(token)) {
        writeSessionExpired(response);   // 直接写 401 响应体
        return;                          // ⚠️ 必须 return，绝不能落到 finally 放行
    }
    // 认证逻辑……
} catch (Exception e) {
    log.error("认证异常", e);             // 只记录，不再向上抛
}
// 注意：这里不再有 finally { doFilter }
filterChain.doFilter(request, response);
```

配合一个专门的响应方法，返回**约定的错误码**：

```java
private void writeSessionExpired(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
        JSON.toJSONString(R.error(ResultCode.SESSION_EXPIRED))
    );
}
```

其中 `ResultCode.SESSION_EXPIRED(4011, "登录状态已失效，请重新登录")`。

**教训**：
> **在 Servlet Filter 里，`finally { filterChain.doFilter(...) }` 是绝对的禁忌。**
> 只要 `doFilter` 在 `finally` 里，任何"中途拦截 + 抛异常"的设计都会失效——异常被吞，请求照样放行。
> 正确姿势：**要么在 filter 里显式 `return`，要么把 `doFilter` 放在正常流程的末尾。**
>
> 这个坑之所以危险，是因为它**静默失效**：代码不报错、日志不告警，你只会以为"黑名单功能做好了"。

---

## 坑 3：推送时序错了，被踢的设备根本收不到"你已被下线"

**现象**：新设备登录后，旧设备**没有弹出**"您的账号已在其他设备登录"的提示，而是**毫无征兆地直接断线 / 请求 401**。用户一脸懵。

**定位**：登录流程里原本的顺序是：

```
1. 校验账号密码
2. 生成新 token
3. resetUserToken(...)     ← 重置 IM token：旧设备的长连接因 token 变更被踢断
4. 推送强制下线通知(220)    ← 已经晚了
```

**根因**：`resetUserToken` 会让 IM 侧判定旧设备的登录态失效，**长连接直接被断**。等我们再去推 220 强制下线通知时，**旧设备已经不在线了，消息送达不了**。

**解法**：**把"推送通知"提到 `resetUserToken` 之前。**

```java
// ✅ 先通知，再重置
replaceLoginSession(...);        // 内含：写新会话 + 踢同类旧会话 + 推送 220 通知
resetUserToken(...);             // 通知已送达，再断旧连接
```

代码里特意留了注释说明原因：

> **必须在重置 IM token 之前推送，否则旧设备可能已因 deviceLevel 抢占而断开，收不到通知。**

**教训**：
> **"让旧设备下线"和"告诉旧设备你为什么下线"是两件事，顺序不能反。**
> 断连是不可逆的，通知必须**在断连之前**送达。这是一类通用模式：**先通知，后生效。**

---

## 配套设计：几个让功能稳得住的细节

修完上面三个坑，还有几处设计值得记录。

### ① 分布式锁串行化同端登录

并发场景：同一账号在同一类型的两个设备上**同时**登录。如果不加锁，两边可能各自读到"没有旧会话"，于是**两个都活着**，互斥失效。

```java
// login:lock:{uid}:{deviceType}
distributedLockHelper.tryLock(lockKey, waitSeconds, leaseSeconds, () -> {
    kickSameTypeSessions(uid, deviceType);   // 先踢同类旧会话
    writeSession(session);                   // 再写新会话
    indexSession(session);                   // 最后建索引
    return null;
}, () -> { /* 获取锁失败的处理 */ });
```

锁内顺序固定为 **踢旧 → 写新 → 建索引**，保证任何时刻同类型下只有一个活跃会话。

### ② 会话续活要限流，别每条请求都写 Redis

每次请求都刷新会话过期时间 = Redis 写放大。加一个节流窗口：

```java
private static final long TOUCH_THROTTLE_MS = 5 * 60 * 1000;   // 5 分钟

public void touch(String token) {
    // 距上次刷新不足 5 分钟 → 直接跳过，不打 Redis
}
```

用 `SessionTouchFilter`（`OncePerRequestFilter`）在请求链路里挂载，只对**携带 token** 的请求生效。

### ③ 失效错误码要能和普通 401 区分开

前端需要知道"**这是被踢下线**，该弹窗提示并跳登录页"，还是"**这是未登录**，静默跳登录页"。

所以用专门的错误码 `4011`（而不是笼统的 401）：

```
ResultCode.SESSION_EXPIRED(4011, "登录状态已失效，请重新登录")
```

前端拦截器统一识别：

```ts
const SESSION_EXPIRED_CODES = [401, 4011]
// 命中 → window.dispatchEvent(new CustomEvent('session-expired', { detail: { reason } }))
```

再由全局弹窗组件监听该事件，弹出**不可关闭**的"账号已在其他设备登录"提示，点击"重新登录"跳登录页。

> 这类"**跨模块的全局副作用**"（清状态、弹窗、跳转）用**自定义事件**解耦，比在拦截器里硬编码 UI 逻辑干净得多。

### ④ 主动登出接口要防越权

新增的 `POST /auth/logout` 如果只接收 `token` 参数就登出，会存在**越权漏洞**——A 可以传 B 的 token 把 B 登出。

```java
if (!SecurityUtils.getUId().equals(session.getUid())) {
    throw new BizException("无权操作该登录会话");
}
```

**当前登录人**必须和**目标会话的归属人**一致。

---

## 最后：为什么这个"小功能"值得写一篇

多端登录管控，需求文档上只有一句话，但它同时碰了四个敏感面：

1. **唯一性**（`jti`）—— token 不唯一，"精确踢除"就是空中楼阁；
2. **过滤器生命周期**（`finally` 放行）—— 这是**静默失效**，最危险；
3. **时序**（先通知后断连）—— 顺序反了，功能"看起来做了"但用户永远收不到；
4. **越权**（登出接口）—— 新接口自带的安全边界。

**一句话总结**：这类"看起来很简单"的需求，真正的成本从来不在**功能实现**，而在**边界条件**和**静默失效**。写完功能后，务必回过头问自己三个问题：

- 我这个前提假设（token 唯一）**真的成立吗**？
- 我这个拦截逻辑**真的会中断流程吗**？
- 我这个"通知"**是在状态变更之前发的吗**？