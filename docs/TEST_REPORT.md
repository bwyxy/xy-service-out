# 好友与消息通知功能测试报告

## 1. 测试概述

| 项目 | 说明 |
|------|------|
| 测试模块 | chat-api/fg-chat |
| 测试范围 | 好友服务、好友控制器、通知服务相关功能 |
| 测试类型 | 单元测试、集成测试 |
| 测试框架 | JUnit 5、Mockito、Spring Boot Test |
| 数据库 | H2 内存数据库（集成测试） |
| 执行命令 | `mvn test -pl chat-api/fg-chat -am -Dnet.bytebuddy.experimental=true` |

## 2. 测试环境

- **JDK**：OpenJDK 23
- **Spring Boot**：3.2.4
- **Maven**：3.9+
- **数据库**：H2（测试）；MySQL 8（生产）
- **外部依赖**：测试中通过 `@MockBean` 禁用 WukongIM 与 Redis 分布式锁

## 3. 单元测试

### 3.1 测试类

**FriendServiceImplTest**（`chat-api/fg-chat/src/test/java/com/chat/fg/service/FriendServiceImplTest.java`）

使用 Mockito 隔离 FriendMapper、FriendRequestMapper、UserService、NotificationService 与 DistributedLockHelper，验证好友服务核心逻辑。

### 3.2 测试用例

| 编号 | 方法 | 场景 | 预期结果 |
|------|------|------|----------|
| UT-01 | `searchUsers_shouldReturnMatchedUsers` | 搜索关键词匹配用户 | 返回匹配的用户列表 |
| UT-02 | `sendFriendRequest_shouldThrowWhenTargetIsSelf` | 用户添加自己为好友 | 抛出 BusinessException："不能添加自己为好友" |
| UT-03 | `sendFriendRequest_shouldThrowWhenAlreadyFriend` | 对方已经是好友 | 抛出 BusinessException："对方已经是您的好友" |
| UT-04 | `sendFriendRequest_shouldCreateRequest` | 正常发送好友请求 | 生成待处理请求，调用 insert 与 notifyUser |
| UT-05 | `handleFriendRequest_shouldAcceptAndCreateFriendRelation` | 同意好友请求 | 请求状态变为已同意，插入两条好友关系 |
| UT-06 | `handleFriendRequest_shouldThrowWhenAlreadyHandled` | 请求已被处理再次操作 | 抛出 BusinessException："该请求已处理，请勿重复操作" |
| UT-07 | `listFriends_shouldReturnFriendsWithUserInfo` | 查询好友列表 | 返回包含好友昵称/头像的列表 |
| UT-08 | `listReceivedRequests_shouldFilterByStatus` | 按状态筛选收到请求 | 返回对应状态的请求及发送者信息 |

### 3.3 单元测试结果

- **用例数**：8
- **通过**：8
- **失败**：0
- **错误**：0
- **跳过**：0

## 4. 集成测试

### 4.1 测试类

**FriendControllerIntegrationTest**（`chat-api/fg-chat/src/test/java/com/chat/fg/controller/FriendControllerIntegrationTest.java`）

通过 `@SpringBootTest` 与 `MockMvc` 启动完整 Spring 上下文，使用 H2 内存数据库验证端到端好友流程。

### 4.2 测试用例

| 编号 | 方法 | 场景 | 预期结果 |
|------|------|------|----------|
| IT-01 | `searchUsers_shouldReturnList` | 已登录用户搜索用户 | 返回 200 与搜索结果 |
| IT-02 | `sendAndHandleFriendRequest_fullFlow` | A 发送请求 → B 查看收到请求 → B 同意 → A 查看好友列表包含 B | 全流程 200，好友关系建立成功 |

### 4.3 集成测试结果

- **用例数**：2
- **通过**：2
- **失败**：0
- **错误**：0
- **跳过**：0

## 5. 测试执行记录

```text
[INFO] Running com.chat.fg.controller.FriendControllerIntegrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.chat.fg.service.FriendServiceImplTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.981 s
```

执行时间：约 5 秒。

## 6. 问题与修复

| 问题 | 原因 | 修复方式 |
|------|------|----------|
| 编译错误："无法取消引用 int" | `ACTION_ACCEPT.equals(action)` 中 action 为 Integer 但 ACTION_ACCEPT 为 int | 使用 `Integer.valueOf(ACTION_ACCEPT).equals(action)` |
| 日志方法找不到 | 继承了 MyBatis 的 `log` 字段，与方法签名冲突 | 使用 SLF4J `Logger LOGGER = LoggerFactory.getLogger(...)` |
| `list` 方法引用不明确 | `userService.list()` 存在多个重载 | 明确使用 `any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)` |
| 测试缺少依赖 | chat-api/fg-chat 未引入 Spring Boot Test、H2 | 在 `chat-api/fg-chat/pom.xml` 中补充依赖 |
| Byte Buddy 与 Java 23 不兼容 | 当前 Byte Buddy 版本未正式支持 JDK 23 | 在 Surefire 插件参数中添加 `-Dnet.bytebuddy.experimental=true` |
| 不必要的桩代码异常 | `@BeforeEach` 中全局 stub 了部分测试用不到的锁 | 移除全局 stub，按需在各测试方法中调用 stub 方法 |
| LocalDateTime 序列化异常 | Jackson 默认将时间序列化为数组 | 新增 `JacksonConfig` 注册 `JavaTimeModule` 并禁用时间戳输出 |
| 集成测试好友列表断言失败 | 切换用户 B 后未切回用户 A 上下文 | 在查询 A 的好友列表前重新设置 SecurityContext 为 A |

## 7. 测试覆盖说明

本次测试重点覆盖：

- 好友搜索、发送、处理、列表查询的核心业务路径。
- 并发控制入口（分布式锁在测试中通过 stub 直接放行）。
- 跨用户端到端流程（集成测试）。
- 异常分支：重复添加、重复处理、添加自己、已是好友。

未覆盖（建议后续补充）：

- `NotificationServiceImpl` 的单元测试。
- 分布式锁真实并发场景下的互斥性测试（需多线程/多进程压测）。
- 前端组件自动化测试。

## 8. 结论

本次实现的好友添加、消息通知与并发控制功能已通过全部 10 个自动化测试（8 个单元测试 + 2 个集成测试），测试构建成功，功能可进入下一阶段的代码审查或集成验证。
