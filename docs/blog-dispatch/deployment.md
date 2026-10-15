# 博客多平台分发模块集成部署指南

## 1. 环境要求

| 组件 | 要求 |
|------|------|
| JDK | 17（项目为 Spring Boot 3.2.4） |
| MySQL | 8.x，需执行 `sql/dispatch.sql` |
| Redis | 用于分发平台按分钟限流，Redis 不可用时自动降级放行并记录告警 |
| Node.js | 18+（`y-micro-app` 前端构建，pnpm） |
| 七牛云 | 复用现有 `QiNiuService`，作为图片迁移的降级图床 |

## 2. 后端集成

### 2.1 代码分布

| 模块 | 内容 |
|------|------|
| `xy-common` | 枚举（平台/状态/正文格式/异常类型）、4 个实体、请求 DTO 与响应 VO |
| `xy-system` | 凭证加解密、HTML→Markdown、图片迁移、内容适配、限流、HTTP 客户端、适配器体系、Mapper/Service、异步执行器、定时调度器、线程池 |
| `xy-admin` | 平台配置、分发任务、数据统计三个 Controller（已接入 `@PreAuthorize`） |

模块为普通 Spring Bean，随现有 `xy-admin` 应用启动自动装配，**无需修改现有文章中心任何代码**。

### 2.2 初始化数据库

```bash
mysql -h<host> -P<port> -u<user> -p <db> < sql/dispatch.sql
```

脚本内容：

1. 建表 `blog_dispatch_platform`、`blog_dispatch_platform_role`、`blog_dispatch_task`、`blog_dispatch_article_stat`。
2. 写入 6 个平台种子数据（默认禁用）。
3. 写入菜单 `2022`~`2032`，挂载在「文章管理」(menu_id = 7) 下。
4. 为 `role_key = 'admin'` 的角色授予全部新菜单。

> 注意：`blog_dispatch_task.article_id` 通过外键关联 `sys_article(article_id)`（级联删除）。若目标库文章表主键名不同，请先调整外键定义。脚本末尾附带回滚语句（默认注释）。

### 2.3 配置项

配置位于 `xy-system/src/main/resources/application-base.yml` 的 `dispatch` 节点：

```yaml
dispatch:
  # 定时任务总开关，false 时 DispatcherScheduler 不装配
  scheduler:
    enabled: true
  # 平台凭证加密密钥，未单独配置时复用项目统一的 AES_KEY
  credential:
    key: ${DISPATCH_AES_KEY:${AES_KEY:}}
  # 自己博客站点地址前缀，用于回传原文地址（留空则不下发原文链接）
  article-url-prefix: ${BLOG_SITE_URL:}
  # 待分发任务兜底扫描
  scan:
    interval-ms: 30000
    batch-size: 50
  # 瞬时异常失败任务的自动重试
  retry:
    interval-ms: 60000
  # 平台运营数据采集时间（默认每天 02:00）
  stat:
    cron: 0 0 2 * * ?
```

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `dispatch.scheduler.enabled` | `DISPATCH_SCHEDULER_ENABLED` | `true` | 定时调度开关 |
| `dispatch.credential.key` | `DISPATCH_AES_KEY`（回退 `AES_KEY`） | 空 | 凭证加密密钥，**生产必须配置** |
| `dispatch.article-url-prefix` | `BLOG_SITE_URL` | 空 | 原文地址前缀 |
| `dispatch.scan.interval-ms` | `DISPATCH_SCAN_INTERVAL_MS` | `30000` | 兜底扫描间隔 |
| `dispatch.scan.batch-size` | `DISPATCH_SCAN_BATCH_SIZE` | `50` | 单次扫描批量 |
| `dispatch.retry.interval-ms` | `DISPATCH_RETRY_INTERVAL_MS` | `60000` | 自动重试间隔 |
| `dispatch.stat.cron` | `DISPATCH_STAT_CRON` | `0 0 2 * * ?` | 运营数据采集 cron |

> 密钥一旦确定后不可随意变更，否则历史密文无法解密（解密失败会返回空凭证并提示重新配置）。生产环境请通过环境变量注入，禁止在配置文件中写死。

### 2.4 依赖的既有能力

- **Spring 定时任务**：`xyAdminApplication` 已标注 `@EnableScheduling`，无需额外开启。
- **线程池**：`DispatchConfig` 提供 `dispatchExecutor`（核心 4 / 最大 16 / 队列 1000 / `CallerRunsPolicy`），任务提交后由其异步执行。
- **图片上传**：复用 `QiNiuService`，需保证七牛相关配置已就绪。

## 3. 前端集成

前端位于 `y-micro-app/apps/y-admin-sub`，路由由菜单驱动，**新增页面无需手动注册路由**，只要 `sys_menu.component` 与文件路径一致即可。

新增文件：

```
src/api/blog/dispatch.ts                                  # 18 个接口封装
src/views/article/dispatch/platform/index.vue             # 分发平台配置页
src/views/article/dispatch/task/index.vue                 # 分发任务列表页
src/views/article/dispatch/stat/index.vue                 # 分发数据统计页
src/components/DispatchDialog/index.vue                   # 分发弹窗（复用于单篇/批量）
```

改动文件（向后兼容）：

```
src/components/BaseTable/BaseTable.vue        # 新增 selectable 属性与 getSelection() 方法
src/views/article/article/index.vue           # 列表增加"分发"操作与"批量分发"工具栏入口
src/views/article/article/operate.vue         # 编辑页增加"分发到第三方平台"按钮
```

`BaseTable` 的 `selectable` 与 `getSelection()` 均为新增能力，默认不开启，不影响既有列表页。

## 4. 构建与启动

```bash
# 后端：编译
export JAVA_HOME=<JDK17_HOME>
mvn -T 1C -DskipTests compile

# 后端：打包 admin
mvn -pl xy-admin -am clean package -DskipTests

# 前端：构建 admin 子应用
cd y-micro-app
pnpm install
pnpm --filter y-admin-sub build
```

## 5. 部署后验证清单

1. 登录 admin，左侧「文章管理」下出现**分发平台配置 / 分发任务 / 分发数据统计**三个菜单。
2. 打开「分发平台配置」，可控性测试任一平台并保存凭证，数据库中 `credential_cipher` 为密文（非明文 JSON）。
3. 在「文章管理 → 文章列表」勾选一篇文章点「分发」，弹窗能列出已启用的平台。
4. 提交后跳转「分发任务」，任务状态由 待分发 → 分发中 → 分发成功/失败 流转，耗时与链接正确回填。
5. 失败任务可一键/批量重试；瞬时异常等待自动重试间隔后自动重跑。
6. 「分发数据统计」能看到聚合与明细；点「立即同步数据」返回更新条数。
7. 用非超管角色登录，验证未授权平台不出现在分发弹窗中。

## 6. 自动化测试

| 测试类 | 用例数 | 覆盖点 |
|--------|--------|--------|
| `DispatchCredentialCryptoTest` | 6 | 凭证加解密、密文不含明文、密钥变更/缺失、非法密文 |
| `ContentAdaptServiceTest` | 9 | HTML→Markdown、摘要回退、长度与标签裁剪、内容违规/超长、脚本清理 |
| `CsdnDispatchAdapterTest` | 15 | CSDN 签名串拼装与 HMAC 结果、请求体组装、草稿/转载、业务码与限流错误、缺凭证快速失败 |
| `BlogDispatchTaskServiceImplTest` | 16 | 单篇/批量提交、去重跳过、平台不可用跳过、重试、任务分页 |
| `DispatchTaskExecutorTest` | 10 | 成功发布、限流/违规/平台禁用/适配器缺失/未知异常、状态并发保护 |
| `BlogDispatchStatServiceImplTest` | 13 | 数据采集回写、发布回传、聚合与分页 |
| `BlogDispatchControllerTest` | 15 | 三个 Controller 的增删改查、批量分发、批量重试/删除、统计同步 |

合计 84 个用例。

```bash
mvn -pl xy-system,xy-admin -am test \
  -Dtest='DispatchCredentialCryptoTest,ContentAdaptServiceTest,CsdnDispatchAdapterTest,BlogDispatchTaskServiceImplTest,DispatchTaskExecutorTest,BlogDispatchStatServiceImplTest,BlogDispatchControllerTest'
```

> 注意：`-am` 不可省略，否则可能使用本地仓库中的旧 `xy-common` 导致找不到新增类。

## 7. 扩展新平台

1. 在 `xy-common` 的 `DispatchPlatformEnum` 增加平台编码与名称。
2. 在 `xy-system/.../dispatch/adapter/impl` 下新增适配器，二选一：

   - **真实实现**：继承 `AbstractDispatchPlatformAdapter`，适合有固定接口地址、需要动态签名的平台，参考 [CsdnDispatchAdapter.java](../xy-system/src/main/java/com/xy/system/dispatch/adapter/impl/CsdnDispatchAdapter.java)（HMAC-SHA256 签名）与 [JuejinDispatchAdapter.java](../xy-system/src/main/java/com/xy/system/dispatch/adapter/impl/JuejinDispatchAdapter.java)（含图床上传）。
   - **模板驱动**：继承 `AbstractTemplateDispatchAdapter`，适合接口地址与请求体都能写成固定模板的平台（知乎、简书、开源中国即为此类），可直接在平台配置里填 `publishUrl` 等参数接入，无需写代码。

   共同需要实现：

   - `platformCode()` / `platformName()`
   - `contentFormat()`：正文格式（MARKDOWN / HTML / PLAIN_TEXT）
   - `requiredCredentialKeys()`：前端配置表单据此渲染凭证输入
   - `validateCredential()`：凭证校验
   - `publish()`：发布逻辑
   - 可选：`supportsImageUpload()` / `uploadContentImage()` / `supportsStatFetch()` / `fetchStats()` / `testConnection()`

3. 标注为 Spring Bean（如 `@Component`），`DispatchAdapterRegistry` 会自动纳管并打印注册日志，无需改注册代码。
4. 在「分发平台配置」页新增该平台记录并配置凭证后启用；角色授权按需勾选。

## 8. 回滚

1. 停用：在「分发平台配置」中禁用所有平台，或在配置中设置 `dispatch.scheduler.enabled: false` 停止后台调度。
2. 菜单与权限：执行 `sql/dispatch.sql` 末尾注释的 `sys_role_menu` / `sys_menu` 删除语句。
3. 数据表：脚本末尾提供 4 张表的 `drop table` 语句（按外键依赖顺序，默认注释）。删除 `blog_dispatch_task` 前请确认可接受任务与统计数据的丢失。

## 9. 已知限制

- 除微信公众平台使用官方草稿箱接口外，其余均依赖 Cookie 走非官方接口，平台前端改版可能导致接口失效，需要相应更新适配器；此时任务会以 `PLATFORM_ERROR` 等类型失败并给出明确提示。
- CSDN 的 `x-ca-key` 与 `appSecret` 为 CSDN 前端内置常量（`203803574` / `9znpamsyl2c7cdrr9sas0le9vbc3r6ba`），随其前端发布包变动；若出现 `X-Ca-Key is not exist` 或签名校验失败，需从 `https://csdnimg.cn/release/md/static/js/app.chunk.*.js` 重新提取并更新 `CsdnDispatchAdapter` 中的常量。
- CSDN 未实现平台图床上传，正文图片统一迁移到自有七牛图床；也未接入运营数据采集（配置了拉取规则的平台才会采集）。
- CSDN 对非会员有每日发文额度限制，批量分发可能被拒；建议先在发布规则中开启 `publishAsDraft` 存草稿。
- 掘金的发布链路为「`article_draft/create` 创建草稿 → `article/publish` 发布草稿」两步；`publishAsDraft` 为真时只做第一步并回传草稿编辑地址（草稿未公开，文章详情页此时不可访问）。掘金要求摘要 50-100 字、至少一个标签，且 `category_id` 必须传字符串、`is_original` 必须传数字 `1`/`0`（传布尔值会被其 Go 服务端反序列化拒绝）。
- 掘金只接受其自有图床（`byteimg.com`）的封面地址，外部图床封面会被参数校验拒绝（`err_no 2 参数错误`），适配器因此不会携带外部封面，文章封面需发布后手动设置。
- 掘金图床为「取上传 token + 对象存储直传」两步流程，当前未接入，正文图片与封面统一迁移到自有七牛图床。
- 掘金与 CSDN 均未实现运营数据采集（`supportsStatFetch` 为 `false`），这两类平台的分发任务不会回传浏览量等数据；模板驱动的知乎/简书/开源中国需在发布规则中配置 `statUrlTemplate` 等字段后才会采集。
- 微信公众平台服务器 IP 需加入平台白名单，否则接口调用会被拒绝。
