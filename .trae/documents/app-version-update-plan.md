# 应用版本发布与客户端检查更新

## Context

目前项目里没有任何版本发布能力：前端连"当前版本号"都不存在（`y-chat-sub/package.json` 是 `0.0.0`，`tauri.conf.json` 是 `1.0.0`，构建时也没注入），桌面端和 Web 端发新版后用户无从感知，只能自己重新下载。

本次要做完整的闭环：**后台按「应用 + 平台」发布版本（含上传安装包）→ 客户端启动或手动触发检查 → 弹窗提示更新（强制 / 可选）**。

已确认的口径：
- 通用多应用设计（`app_key` 区分应用，`platform` 区分平台），未来可加新应用
- 桌面端只做「提示 + 跳转系统浏览器下载」，**不接 tauri-plugin-updater 自动更新**
- 安装包上传到七牛云（复用现有 `QiNiuService`），后台自动回填下载地址
- 更新策略两档：强制 / 可选

## 设计要点

### 数据模型：新建 `sys_app_version`

沿用 `sql/chat.sql` 的书写风格（`engine=InnoDB`、`utf8mb4`、每列带 comment）。

| 字段 | 说明 |
|---|---|
| id | 主键自增 |
| app_key | 应用标识，本期用 `chat` |
| platform | `web` / `windows` / `macos` |
| version_name | 展示版本号，如 `1.2.0` |
| version_code | 数字版本码，如 `10200`，**版本大小比较只用它** |
| file_name / file_size | 安装包文件名与字节数 |
| download_url | 七牛外链；`web` 平台留空（前端直接刷新页面） |
| changelog | 更新说明，多行 |
| force_update | `0` 可选 / `1` 强制 |
| status | `0` 上架 / `1` 下架（只有上架的参与更新判定） |
| create_by / update_by / create_time / update_time / deleted | 沿用 `BaseEntity` 约定 |

索引：`uk_app_platform_code(app_key, platform, version_code)` 防同一版本重复发布，`idx_app_platform(app_key, platform, status)` 供检查更新查询。

`app_key` 暂不建应用表，前端用常量映射（`chat` → 飞鸽即时通讯），需要时再升级。

### 版本判定规则

客户端上报 `appKey` + `platform` + `versionCode`；后端在 `status='0' and deleted=0` 的记录里按 `version_code desc` 取第一条作为最新版，然后：

```
hasUpdate   = latest.versionCode > 上报的 versionCode
forceUpdate = hasUpdate && latest.force_update == '1'
```

无任何记录时返回 `hasUpdate=false`（不报错）。只回一条最新版，客户端不做多版本遍历。

### 平台的"更新"语义差异

| 平台 | 按钮行为 |
|---|---|
| web | `location.reload()`，拿最新静态资源；下载地址可留空 |
| windows / macos | 用系统默认浏览器打开 `download_url` |

## 实施步骤

### 1. 建表与菜单

新建 `sql/app-version.sql`：建表 + 菜单 + 角色授权 + 注释版回滚脚本。

菜单占用 **2200-2204**（已扫过 `sql/*.sql`，现用最大 `menu_id` 是 2106）：

- `2200` 应用版本管理，parent=1，component `system/appVersion/index`，perms `system:appVersion:list`
- `2201/2202/2203/2204` = 新增 / 修改 / 删除 / 上传安装包按钮
- `sys_role_menu` 按 `role_key='admin'` 授权

### 2. 后端分层

沿用 `SysDictData` 那套样板（Controller 只在 xy-admin，Service/Mapper 在 xy-system，Entity/VO 在 xy-common）：

| 文件 | 说明 |
|---|---|
| `xy-common/.../entity/AppVersion.java` | `@TableName("sys_app_version") extends BaseEntity` |
| `xy-common/.../vo/AppUpdateVO.java` | 客户端响应：hasUpdate / latestVersion / latestVersionCode / forceUpdate / downloadUrl / fileSize / changelog |
| `xy-system/.../mapper/AppVersionMapper.java` | `extends BaseMapper<AppVersion>`，无 XML |
| `xy-system/.../service/ISysAppVersionService.java` + impl | 见下 |

Service 方法：

```java
PageResult<AppVersion> selectPage(AppVersion query);                      // 管理端列表
AppUpdateVO checkUpdate(String appKey, String platform, Integer versionCode); // 客户端
String uploadPackage(Long id, MultipartFile file);                         // 回填 url/name/size
```

分页返回用 `PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize())`。

### 3. 管理端接口 + 通用文件上传

- 新增 `xy-admin/.../controller/AppVersionController.java`，`@RequestMapping("/system/app-version")`：`POST /list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{id}`、`POST /{id}/package`，逐个挂 `@PreAuthorize("@auth.hasPermi('system:appVersion:xxx')")` 和 `@Log`
- 扩展 [UploadController.java](file:///Users/xiaoyi/me-project/xy-service-out/xy-admin/src/main/java/com/xy/admin/controller/UploadController.java)：新增 `POST /upload/file`，直接复用已有的 `qiNiuService.upload(MultipartFile)`，加后缀白名单（exe/dmg/zip/apk/deb/rpm/AppImage/msi/msix）
- 放开上传体积：[application-base.yml](file:///Users/xiaoyi/me-project/xy-service-out/xy-system/src/main/resources/application-base.yml#L29-L31) 的 `max-file-size/max-request-size` 由 10MB/20MB 提到 100MB/120MB。这是 xy-system jar 里的共享基础配置，xy-admin 与 fg-chat 共用，改一处全局生效（比在 xy-admin 侧的 profile 覆盖更确定）

### 4. 客户端检查更新接口（fg-chat）

新增 `customer-api/fg-chat/.../controller/AppUpdateController.java`：

```java
@Anonymous                                   // 免登录，见下方说明
@GetMapping("/app/version/check")
public R<AppUpdateVO> check(@RequestParam String appKey,
                            @RequestParam String platform,
                            @RequestParam Integer versionCode)
```

免登录是靠 [PermitAllUrlProperties.java](file:///Users/xiaoyi/me-project/xy-service-out/xy-framework/src/main/java/com/xy/framework/config/properties/PermitAllUrlProperties.java#L37-L68) 扫描 `@Anonymous`（`com.xy.common.annotation.Anonymous`）自动加白名单，**不用改任何 SecurityConfig 或过滤器**。这样未登录也能收到强更提示。

### 5. admin 前端

- `src/api/appVersion.ts`：`listAppVersion / getAppVersion / addAppVersion / updateAppVersion / delAppVersion / uploadAppPackage`
- `src/views/system/appVersion/index.vue`：基于 [BaseTable.vue](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-admin-sub/src/components/BaseTable/BaseTable.vue) 的列表 + `el-dialog` 表单（应用、平台、版本号、版本码、强更开关、更新说明、安装包上传），操作列含「上架 / 下架」
- 安装包上传用 `el-upload` 配自定义 `http-request` 调 `/upload/file`（参考 `src/components/ImageUpload/ImageUpload.vue` 的写法，但这里是单文件、非图片，不直接复用该组件）

### 6. chat 前端

**a) 建立版本号来源**（这是前置条件）

- [vite.config.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/vite.config.ts#L17-L82)：读 `package.json` 的 version，注入
  ```ts
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __APP_VERSION_CODE__: String(major * 10000 + minor * 100 + patch),
  }
  ```
  该文件用 `mergeConfig(viteBaseConfig, config)`，加 `define` 无冲突；qiankun 只做运行时挂载，不影响子应用自身的 define
- `src/vite-env.d.ts` 补 `declare const __APP_VERSION__: string` / `__APP_VERSION_CODE__: number`
- `package.json` 的 version 由 `0.0.0` 改为与 `tauri.conf.json` 一致的 `1.0.0`

**b) 新增文件**

| 文件 | 作用 |
|---|---|
| `src/utils/platform.ts` | `detectPlatform()` → `web` / `windows` / `macos`，复用 [tauri.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/utils/tauri.ts#L7-L19) 的 `isTauri()` / `isMacOS()` |
| `src/api/appVersion.ts` | 调 `/app/version/check` |
| `src/store/appVersion.ts` | 保存检查结果与弹窗开关 |
| `src/components/CheckUpdateDialog/index.vue` | 更新弹窗 |

**c) 挂载与入口**

- 在 `src/layout/index.vue` 里与 `<force-logout-dialog />` 同级挂 `<check-update-dialog />`，并在 `onMounted` 静默检查一次（失败不打扰用户）
- `src/layout/MoreMenu/MoreMenu.vue` 增加「检查更新」项，手动触发时用 `ElMessage` 给出反馈

**d) 弹窗交互**（样板 [ForceLogoutDialog](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/components/ForceLogoutDialog/index.vue)）

- 强制更新：`show-close=false`、`close-on-click-modal/press-escape=false`，只留「立即更新」
- 可选更新：「立即更新」+「稍后再说」，点稍后把 `version_code` 记进 `sessionStorage`，本次会话不再弹
- 更新动作按平台分支：web → `location.reload()`；桌面端 → 打开下载地址

**e) Tauri 端接线（为了让桌面端能唤起系统浏览器）**

Tauri 会拦截 `window.open`（不会唤起系统浏览器），需加官方 opener 插件：

- [Cargo.toml](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src-tauri/Cargo.toml#L25) 追加 `tauri-plugin-opener = "2"`
- [lib.rs](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src-tauri/src/lib.rs#L146) 的 `tauri::Builder::default()` 后追加 `.plugin(tauri_plugin_opener::init())`
- [capabilities/default.json](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src-tauri/capabilities/default.json#L9-L18) 的 `permissions` 追加 `"opener:default"`
- `package.json` 加 `@tauri-apps/plugin-opener`，调用用动态 `import('@tauri-apps/plugin-opener')` 的 `openUrl`（与现有 tauri.ts 的动态 import 风格一致）

## 需要注意的点

1. **版本号要保持一致**：`package.json` 是前端运行时版本的唯一来源，`tauri.conf.json` 的 version 决定安装包版本号，两者目前需发版时手工同步（Tauri 不支持从 package.json 引用）
2. **服务器 nginx 需放行大文件**：上传安装包会走 `client_max_body_size`，仓库内无 nginx 配置，上线时需在服务器侧放宽
3. **唯一索引会拦截重复版本**：同一 `app_key + platform + version_code` 重复发布会被拦，属预期行为
4. 本次不改变更检测时机之外的既有行为，也不动 `fg-chat` 的鉴权链路

## 验证方式

1. **后端**：`mvn -DskipTests compile` 全模块通过；启动后 `curl` 免登检查接口
   ```
   curl "http://localhost:9080/app/version/check?appKey=chat&platform=windows&versionCode=10000"
   ```
   不带 token 应返回 `code=200`，无版本记录时 `hasUpdate=false`
2. **上传**：后台传一个 >10MB 的安装包，确认不再 413、七牛外链可直接下载、`download_url/file_size` 已回填
3. **后台页面**：菜单能打开，列表/新增/编辑/上架下架正常，重复 version_code 被拦
4. **客户端**：发布一个 `version_code` 大于本地的版本
   - 强制更新：弹窗无关闭按钮、遮罩点不掉
   - 可选更新：可关闭，点「稍后」后同会话不再弹，手动「检查更新」仍能查到
   - Web 端点「立即更新」触发 `location.reload()`；桌面端点按钮唤起系统默认浏览器打开下载地址
5. **前端类型检查**：`pnpm vue-tsc --noEmit` 零报错
